package com.siseg.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.siseg.dto.EnderecoCepResponseDTO;
import com.siseg.dto.geocoding.Coordinates;
import com.siseg.dto.geocoding.NominatimResponse;
import com.siseg.dto.geocoding.OsrmRoute;
import com.siseg.dto.geocoding.OsrmRouteResponse;
import com.siseg.dto.geocoding.RouteResult;
import com.siseg.dto.geocoding.ViaCepResponse;
import com.siseg.model.Endereco;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.util.PolylineDecoder;
import com.siseg.util.VehicleConstants;

@Service
public class GeocodingService {
    
    private static final Logger logger = Logger.getLogger(GeocodingService.class.getName());
    
    private final WebClient nominatimClient;
    private final WebClient viacepClient;
    private final WebClient osrmClient;
    private final Map<String, Coordinates> cache = new ConcurrentHashMap<>();
    
    private volatile long lastRequestTime = 0;
    
    private final int osrmTimeout;
    private final int osrmMaxRetries;
    private final long osrmRetryDelay;
    
    public GeocodingService(@Value("${geocoding.nominatim.baseUrl}") String nominatimBaseUrl,
                           @Value("${geocoding.viacep.baseUrl}") String viacepBaseUrl,
                           @Value("${geocoding.osrm.baseUrl}") String osrmBaseUrl,
                           @Value("${geocoding.nominatim.userAgent}") String nominatimUserAgent,
                           @Value("${geocoding.osrm.timeout:5000}") int osrmTimeout,
                           @Value("${geocoding.osrm.retry.maxAttempts:3}") int osrmMaxRetries,
                           @Value("${geocoding.osrm.retry.delay:1000}") long osrmRetryDelay) {
        this.osrmTimeout = osrmTimeout;
        this.osrmMaxRetries = osrmMaxRetries;
        this.osrmRetryDelay = osrmRetryDelay;
        this.nominatimClient = WebClient.builder()
                .baseUrl(nominatimBaseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, nominatimUserAgent)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9")
                .build();
        
        this.viacepClient = WebClient.builder()
                .baseUrl(viacepBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        this.osrmClient = WebClient.builder()
                .baseUrl(osrmBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
    
    public void geocodeAddress(Endereco endereco) {
        if (!isEnderecoValidoParaGeocodificacao(endereco)) {
            return;
        }
        
        if (enderecoJaPossuiCoordenadas(endereco)) {
            return;
        }
        
        String enderecoFormatado = endereco.toGeocodingString();
        
        if (aplicarCoordenadasDoCache(endereco, enderecoFormatado)) {
            return;
        }
        
        try {
            String enderecoCompleto = atualizarEnderecoComCep(endereco, enderecoFormatado);
            Optional<Coordinates> coordenadas = geocodeWithNominatim(enderecoCompleto);
            salvarCoordenadas(endereco, enderecoFormatado, enderecoCompleto, coordenadas);
        } catch (Exception e) {
            logger.warning("Erro ao geocodificar endereço '" + enderecoFormatado + "': " + e.getMessage());
        }
    }
    
    private boolean isEnderecoValidoParaGeocodificacao(Endereco endereco) {
        if (endereco == null || !endereco.isCompleto()) {
            logger.warning("Endereço nulo ou incompleto para geocodificação");
            return false;
        }
        return true;
    }
    
    private boolean enderecoJaPossuiCoordenadas(Endereco endereco) {
        if (endereco.getLatitude() != null && endereco.getLongitude() != null) {
            logger.fine("Endereço já possui coordenadas: " + endereco.toGeocodingString());
            return true;
        }
        return false;
    }
    
    private boolean aplicarCoordenadasDoCache(Endereco endereco, String enderecoFormatado) {
        if (cache.containsKey(enderecoFormatado)) {
            Coordinates coords = cache.get(enderecoFormatado);
            endereco.setLatitude(coords.getLatitude());
            endereco.setLongitude(coords.getLongitude());
            logger.fine("Coordenadas encontradas no cache para: " + enderecoFormatado);
            return true;
        }
        return false;
    }
    
    private String atualizarEnderecoComCep(Endereco endereco, String enderecoFormatado) {
        if (!temCepValido(endereco)) {
            return enderecoFormatado;
        }
        
        Optional<EnderecoCepResponseDTO> enderecoViaCepDTO = buscarEnderecoPorCep(endereco.getCep());
        if (enderecoViaCepDTO.isEmpty()) {
            return enderecoFormatado;
        }
        
        EnderecoCepResponseDTO dto = enderecoViaCepDTO.get();
        String logradouroOriginal = endereco.getLogradouro();
        
        atualizarCamposDoEndereco(endereco, dto);
        String enderecoCompleto = construirEnderecoCompleto(dto, endereco);
        logarAtualizacaoEndereco(logradouroOriginal, dto.getLogradouro(), enderecoCompleto);
        
        return enderecoCompleto;
    }
    
    private boolean temCepValido(Endereco endereco) {
        return endereco.getCep() != null && endereco.getCep().length() == 8;
    }
    
    private void atualizarCamposDoEndereco(Endereco endereco, EnderecoCepResponseDTO dto) {
        endereco.setLogradouro(dto.getLogradouro());
        if (temBairroValido(dto)) {
            endereco.setBairro(dto.getBairro());
        }
        endereco.setCidade(dto.getCidade());
        endereco.setEstado(dto.getEstado().toUpperCase());
    }
    
    private boolean temBairroValido(EnderecoCepResponseDTO dto) {
        return dto.getBairro() != null && !dto.getBairro().isEmpty();
    }
    
    private void logarAtualizacaoEndereco(String logradouroOriginal, String logradouroNovo, String enderecoCompleto) {
        if (!logradouroOriginal.equals(logradouroNovo)) {
            logger.info("Endereço corrigido via CEP: logradouro atualizado de '" + 
                       logradouroOriginal + "' para '" + logradouroNovo + 
                       "'. Endereço completo: " + enderecoCompleto);
        } else {
            logger.info("Endereço validado via CEP: " + enderecoCompleto);
        }
    }
    
    private void salvarCoordenadas(Endereco endereco, String enderecoFormatado, 
                                   String enderecoCompleto, Optional<Coordinates> coordenadas) {
        if (coordenadas.isEmpty()) {
            logger.warning("Não foi possível geocodificar endereço: " + enderecoCompleto);
            return;
        }
        
        Coordinates coords = coordenadas.get();
        endereco.setLatitude(coords.getLatitude());
        endereco.setLongitude(coords.getLongitude());
        
        cache.put(enderecoFormatado, coords);
        if (!enderecoCompleto.equals(enderecoFormatado)) {
            cache.put(enderecoCompleto, coords);
        }
        
        logger.info("Coordenadas geocodificadas e salvas: " + enderecoCompleto);
    }
    
    private String construirEnderecoCompleto(EnderecoCepResponseDTO dto, Endereco endereco) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(endereco.getLogradouro()).append(", ");
        sb.append(endereco.getNumero());
        
        adicionarComplemento(sb, endereco);
        adicionarBairro(sb, endereco);
        
        sb.append(", ").append(endereco.getCidade());
        sb.append(", ").append(endereco.getEstado());
        sb.append(", Brasil");
        
        return sb.toString();
    }
    
    private void adicionarComplemento(StringBuilder sb, Endereco endereco) {
        if (temComplemento(endereco)) {
            sb.append(", ").append(endereco.getComplemento());
        }
    }
    
    private boolean temComplemento(Endereco endereco) {
        return endereco.getComplemento() != null && !endereco.getComplemento().trim().isEmpty();
    }
    
    private void adicionarBairro(StringBuilder sb, Endereco endereco) {
        if (temBairro(endereco)) {
            sb.append(", ").append(endereco.getBairro());
        }
    }
    
    private boolean temBairro(Endereco endereco) {
        return endereco.getBairro() != null && !endereco.getBairro().isEmpty();
    }
    
    public Optional<EnderecoCepResponseDTO> buscarEnderecoPorCep(String cep) {
        try {
            String cepLimpo = limparCep(cep);
            
            if (!isCepValido(cepLimpo, cep)) {
                return Optional.empty();
            }
            
            ViaCepResponse response = buscarCepNoViaCep(cepLimpo);
            
            if (isRespostaViaCepValida(response)) {
                return criarEnderecoCepResponseDTO(response, cepLimpo);
            }
            
            logger.warning("CEP não encontrado ou erro na resposta ViaCEP: " + cep);
            return Optional.empty();
            
        } catch (Exception e) {
            logger.warning("Erro ao buscar CEP no ViaCEP: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    private String limparCep(String cep) {
        return cep.replaceAll("[^0-9]", "");
    }
    
    private boolean isCepValido(String cepLimpo, String cepOriginal) {
        if (cepLimpo.length() != 8) {
            logger.warning("CEP inválido: " + cepOriginal);
            return false;
        }
        return true;
    }
    
    private ViaCepResponse buscarCepNoViaCep(String cepLimpo) {
        return viacepClient.get()
                .uri("/ws/{cep}/json/", cepLimpo)
                .retrieve()
                .bodyToMono(ViaCepResponse.class)
                .block();
    }
    
    private boolean isRespostaViaCepValida(ViaCepResponse response) {
        return response != null && response.getErro() == null && response.getLogradouro() != null;
    }
    
    private Optional<EnderecoCepResponseDTO> criarEnderecoCepResponseDTO(ViaCepResponse response, String cepLimpo) {
        EnderecoCepResponseDTO dto = new EnderecoCepResponseDTO();
        dto.setLogradouro(response.getLogradouro());
        dto.setBairro(response.getBairro());
        dto.setCidade(response.getLocalidade());
        dto.setEstado(response.getUf());
        dto.setCep(cepLimpo);
        return Optional.of(dto);
    }
    
    
    private Optional<Coordinates> geocodeWithNominatim(String endereco) {
        try {
            respeitarRateLimit();
            NominatimResponse[] responses = buscarNoNominatim(endereco);
            
            if (temRespostasValidas(responses)) {
                return extrairCoordenadas(responses[0], endereco);
            }
            
            logger.warning("Nenhum resultado encontrado para: " + endereco);
            return Optional.empty();
            
        } catch (WebClientException e) {
            logger.warning("Erro de conexão com Nominatim: " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.warning("Erro ao geocodificar com Nominatim: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    private NominatimResponse[] buscarNoNominatim(String endereco) {
        return nominatimClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", endereco)
                        .queryParam("format", "json")
                        .queryParam("limit", "1")
                        .queryParam("addressdetails", "1")
                        .build())
                .retrieve()
                .bodyToMono(NominatimResponse[].class)
                .block();
    }
    
    private boolean temRespostasValidas(NominatimResponse[] responses) {
        return responses != null && responses.length > 0;
    }
    
    private Optional<Coordinates> extrairCoordenadas(NominatimResponse response, String endereco) {
        if (!temCoordenadasValidas(response)) {
            return Optional.empty();
        }
        
        BigDecimal latitude = new BigDecimal(response.getLat());
        BigDecimal longitude = new BigDecimal(response.getLon());
        Coordinates coords = new Coordinates(latitude, longitude);
        
        logger.info("Geocodificação bem-sucedida: " + endereco + " -> (" + latitude + ", " + longitude + ")");
        return Optional.of(coords);
    }
    
    private boolean temCoordenadasValidas(NominatimResponse response) {
        return response.getLat() != null && response.getLon() != null;
    }
    
    private void respeitarRateLimit() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (timeSinceLastRequest < 1000) {
            try {
                Thread.sleep(1000 - timeSinceLastRequest);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        lastRequestTime = System.currentTimeMillis();
    }
    
    public Optional<RouteResult> calculateRoute(BigDecimal origemLat, BigDecimal origemLon,
                                               BigDecimal destinoLat, BigDecimal destinoLon,
                                               String profile) {
        return calculateRoute(origemLat, origemLon, destinoLat, destinoLon, profile, false);
    }
    
    public Optional<RouteResult> calculateRoute(BigDecimal origemLat, BigDecimal origemLon,
                                               BigDecimal destinoLat, BigDecimal destinoLon,
                                               String profile, boolean includeWaypoints) {
        if (origemLat == null || origemLon == null || destinoLat == null || destinoLon == null) {
            logger.warning("Coordenadas inválidas para cálculo de rota");
            return Optional.empty();
        }
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= osrmMaxRetries; attempt++) {
            try {
                Optional<RouteResult> result = calcularRotaComOSRM(origemLat, origemLon, destinoLat, destinoLon, profile, includeWaypoints);
                
                if (result.isPresent()) {
                    if (attempt > 1) {
                        logger.info("Rota calculada com sucesso na tentativa " + attempt);
                    }
                    return result;
                }
                
            } catch (WebClientResponseException e) {
                lastException = e;
                int statusCode = e.getStatusCode().value();
                String errorBody = e.getResponseBodyAsString();
                
                logger.warning(String.format(
                    "Erro ao calcular rota OSRM (tentativa %d/%d): Status %d - %s",
                    attempt, osrmMaxRetries, statusCode, errorBody != null ? errorBody : e.getMessage()
                ));
                
                if (statusCode >= 400 && statusCode < 500) {
                    logger.severe("Erro do cliente (4xx), não será feito retry: " + statusCode);
                    break;
                }
                
            } catch (WebClientException e) {
                lastException = e;
                logger.warning(String.format(
                    "Erro de conexão com OSRM (tentativa %d/%d): %s",
                    attempt, osrmMaxRetries, e.getMessage()
                ));
                
            } catch (Exception e) {
                lastException = e;
                logger.warning(String.format(
                    "Erro inesperado ao calcular rota OSRM (tentativa %d/%d): %s",
                    attempt, osrmMaxRetries, e.getMessage()
                ));
            }
            
            if (attempt < osrmMaxRetries) {
                try {
                    long delay = osrmRetryDelay * attempt;
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warning("Thread interrompida durante retry");
                    break;
                }
            }
        }
        
        logger.severe(String.format(
            "Falha ao calcular rota OSRM após %d tentativas. Último erro: %s",
            osrmMaxRetries, lastException != null ? lastException.getMessage() : "desconhecido"
        ));
        
        return Optional.empty();
    }
    
    private Optional<RouteResult> calcularRotaComOSRM(BigDecimal origemLat, BigDecimal origemLon,
                                                     BigDecimal destinoLat, BigDecimal destinoLon,
                                                     String profile, boolean includeWaypoints) {
        String coordinates = formatarCoordenadas(origemLat, origemLon, destinoLat, destinoLon);
        String routeProfile = obterProfile(profile);
        
        OsrmRouteResponse response = buscarRotaNoOSRM(routeProfile, coordinates, includeWaypoints);
        
        if (!isRespostaOSRMValida(response)) {
            return Optional.empty();
        }
        
        OsrmRoute route = response.getRoutes().get(0);
        return criarRouteResult(route, includeWaypoints);
    }
    
    private String formatarCoordenadas(BigDecimal origemLat, BigDecimal origemLon,
                                       BigDecimal destinoLat, BigDecimal destinoLon) {
        return String.format("%s,%s;%s,%s", origemLon, origemLat, destinoLon, destinoLat);
    }
    
    private String obterProfile(String profile) {
        return (profile != null && !profile.isEmpty()) ? profile : "driving";
    }
    
    private OsrmRouteResponse buscarRotaNoOSRM(String routeProfile, String coordinates, boolean includeWaypoints) {
        return osrmClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/route/v1/{profile}/{coordinates}")
                        .queryParam("overview", includeWaypoints ? "full" : "false")
                        .queryParam("geometries", includeWaypoints ? "geojson" : "polyline")
                        .queryParam("alternatives", "false")
                        .queryParam("steps", "false")
                        .build(routeProfile, coordinates))
                .retrieve()
                .bodyToMono(OsrmRouteResponse.class)
                .block(Duration.ofMillis(osrmTimeout));
    }
    
    private boolean isRespostaOSRMValida(OsrmRouteResponse response) {
        if (response == null || response.getCode() == null || !"Ok".equals(response.getCode())) {
            logger.warning("OSRM retornou código inválido: " + (response != null ? response.getCode() : "null"));
            return false;
        }
        
        if (response.getRoutes() == null || response.getRoutes().isEmpty()) {
            logger.warning("OSRM não retornou rotas");
            return false;
        }
        
        return true;
    }
    
    private Optional<RouteResult> criarRouteResult(OsrmRoute route, boolean includeWaypoints) {
        BigDecimal distanciaKm = calcularDistanciaKm(route);
        int tempoMinutos = calcularTempoMinutos(route);
        List<Coordinates> waypoints = includeWaypoints ? extrairWaypoints(route) : null;
        
        RouteResult result = new RouteResult(distanciaKm, tempoMinutos, waypoints);
        logarRotaCalculada(distanciaKm, tempoMinutos, waypoints);
        return Optional.of(result);
    }
    
    private BigDecimal calcularDistanciaKm(OsrmRoute route) {
        double distanciaMetros = route.getDistance();
        return BigDecimal.valueOf(distanciaMetros / 1000.0)
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private int calcularTempoMinutos(OsrmRoute route) {
        double tempoSegundos = route.getDuration();
        return (int) Math.ceil(tempoSegundos / 60.0);
    }
    
    private void logarRotaCalculada(BigDecimal distanciaKm, int tempoMinutos, List<Coordinates> waypoints) {
        String waypointsInfo = waypoints != null ? ", " + waypoints.size() + " waypoints" : "";
        logger.fine("Rota calculada via OSRM: " + distanciaKm + " km, " + tempoMinutos + " min" + waypointsInfo);
    }
    
    private List<Coordinates> extrairWaypoints(OsrmRoute route) {
        if (temCoordenadasNaRota(route)) {
            return extrairWaypointsDeCoordenadas(route.getCoordinates());
        }
        
        return extrairWaypointsDeGeometry(route);
    }
    
    private boolean temCoordenadasNaRota(OsrmRoute route) {
        List<List<Double>> coordinates = route.getCoordinates();
        return coordinates != null && !coordinates.isEmpty();
    }
    
    private List<Coordinates> extrairWaypointsDeCoordenadas(List<List<Double>> coordinates) {
        List<Coordinates> waypoints = new ArrayList<>();
        for (List<Double> coord : coordinates) {
            if (isCoordenadaValida(coord)) {
                BigDecimal longitude = BigDecimal.valueOf(coord.get(0));
                BigDecimal latitude = BigDecimal.valueOf(coord.get(1));
                waypoints.add(new Coordinates(latitude, longitude));
            }
        }
        return waypoints;
    }
    
    private boolean isCoordenadaValida(List<Double> coord) {
        return coord != null && coord.size() >= 2;
    }
    
    private List<Coordinates> extrairWaypointsDeGeometry(OsrmRoute route) {
        String geometryStr = route.getGeometryAsString();
        if (temGeometryValida(geometryStr)) {
            return PolylineDecoder.decode(geometryStr);
        }
        return new ArrayList<>();
    }
    
    private boolean temGeometryValida(String geometryStr) {
        return geometryStr != null && !geometryStr.isEmpty();
    }
    
    public String obterProfileOSRM(TipoVeiculo tipoVeiculo) {
        return VehicleConstants.getOsrmProfile(tipoVeiculo);
    }
    
    public void clearCache() {
        cache.clear();
    }
}

