package com.siseg.service;

import java.math.BigDecimal;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.siseg.dto.EnderecoCepResponseDTO;
import com.siseg.dto.geocoding.Coordinates;
import com.siseg.dto.geocoding.NominatimResponse;
import com.siseg.dto.geocoding.OsrmRoute;
import com.siseg.dto.geocoding.OsrmRouteResponse;
import com.siseg.dto.geocoding.RouteResult;
import com.siseg.dto.geocoding.ViaCepResponse;
import com.siseg.model.Endereco;
import com.siseg.util.PolylineDecoder;

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
        if (endereco == null || !endereco.isCompleto()) {
            logger.warning("Endereço nulo ou incompleto para geocodificação");
            return;
        }
        
        if (endereco.getLatitude() != null && endereco.getLongitude() != null) {
            logger.fine("Endereço já possui coordenadas: " + endereco.toGeocodingString());
            return;
        }
        
        String enderecoFormatado = endereco.toGeocodingString();
        
        if (cache.containsKey(enderecoFormatado)) {
            Coordinates coords = cache.get(enderecoFormatado);
            endereco.setLatitude(coords.getLatitude());
            endereco.setLongitude(coords.getLongitude());
            logger.fine("Coordenadas encontradas no cache para: " + enderecoFormatado);
            return;
        }
        
        try {
            String enderecoCompleto = enderecoFormatado;
            if (endereco.getCep() != null && endereco.getCep().length() == 8) {
                Optional<String> enderecoViaCep = buscarEnderecoPorCepString(endereco.getCep());
                if (enderecoViaCep.isPresent()) {
                    enderecoCompleto = construirEnderecoCompleto(enderecoViaCep.get(), endereco);
                    logger.info("Endereço encontrado via CEP: " + enderecoCompleto);
                }
            }
            
            Optional<Coordinates> coordenadas = geocodeWithNominatim(enderecoCompleto);
            
            if (coordenadas.isPresent()) {
                endereco.setLatitude(coordenadas.get().getLatitude());
                endereco.setLongitude(coordenadas.get().getLongitude());
                cache.put(enderecoFormatado, coordenadas.get());
                if (!enderecoCompleto.equals(enderecoFormatado)) {
                    cache.put(enderecoCompleto, coordenadas.get());
                }
                logger.info("Coordenadas geocodificadas e salvas: " + enderecoFormatado);
            } else {
                logger.warning("Não foi possível geocodificar endereço: " + enderecoFormatado);
            }
            
        } catch (Exception e) {
            logger.warning("Erro ao geocodificar endereço '" + enderecoFormatado + "': " + e.getMessage());
        }
    }
    
    private String construirEnderecoCompleto(String enderecoViaCep, Endereco endereco) {
        StringBuilder sb = new StringBuilder();
        
        String logradouroViaCep = enderecoViaCep.split(",")[0].trim();
        sb.append(logradouroViaCep).append(", ");
        sb.append(endereco.getNumero());
        
        if (endereco.getComplemento() != null && !endereco.getComplemento().trim().isEmpty()) {
            sb.append(", ").append(endereco.getComplemento());
        }
        
        String[] partes = enderecoViaCep.split(",", 2);
        if (partes.length > 1) {
            sb.append(", ").append(partes[1].trim());
        }
        
        return sb.toString();
    }
    
    public Optional<EnderecoCepResponseDTO> buscarEnderecoPorCep(String cep) {
        try {
            String cepLimpo = cep.replaceAll("[^0-9]", "");
            
            if (cepLimpo.length() != 8) {
                logger.warning("CEP inválido: " + cep);
                return Optional.empty();
            }
            
            ViaCepResponse response = viacepClient.get()
                    .uri("/ws/{cep}/json/", cepLimpo)
                    .retrieve()
                    .bodyToMono(ViaCepResponse.class)
                    .block();
            
            if (response != null && response.getErro() == null && response.getLogradouro() != null) {
                EnderecoCepResponseDTO dto = new EnderecoCepResponseDTO();
                dto.setLogradouro(response.getLogradouro());
                dto.setBairro(response.getBairro());
                dto.setCidade(response.getLocalidade());
                dto.setEstado(response.getUf());
                dto.setCep(cepLimpo);
                return Optional.of(dto);
            } else {
                logger.warning("CEP não encontrado ou erro na resposta ViaCEP: " + cep);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.warning("Erro ao buscar CEP no ViaCEP: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    private Optional<String> buscarEnderecoPorCepString(String cep) {
        Optional<EnderecoCepResponseDTO> dtoOpt = buscarEnderecoPorCep(cep);
        if (dtoOpt.isPresent()) {
            EnderecoCepResponseDTO dto = dtoOpt.get();
            StringBuilder endereco = new StringBuilder();
            endereco.append(dto.getLogradouro());
            
            if (dto.getBairro() != null && !dto.getBairro().isEmpty()) {
                endereco.append(", ").append(dto.getBairro());
            }
            
            if (dto.getCidade() != null && !dto.getCidade().isEmpty()) {
                endereco.append(", ").append(dto.getCidade());
            }
            
            if (dto.getEstado() != null && !dto.getEstado().isEmpty()) {
                endereco.append(", ").append(dto.getEstado());
            }
            
            endereco.append(", Brasil");
            
            return Optional.of(endereco.toString());
        }
        return Optional.empty();
    }
    
    private Optional<Coordinates> geocodeWithNominatim(String endereco) {
        try {
            respeitarRateLimit();
            
            NominatimResponse[] responses = nominatimClient.get()
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
            
            if (responses != null && responses.length > 0) {
                NominatimResponse response = responses[0];
                if (response.getLat() != null && response.getLon() != null) {
                    BigDecimal latitude = new BigDecimal(response.getLat());
                    BigDecimal longitude = new BigDecimal(response.getLon());
                    
                    Coordinates coords = new Coordinates(latitude, longitude);
                    logger.info("Geocodificação bem-sucedida: " + endereco + " -> (" + latitude + ", " + longitude + ")");
                    return Optional.of(coords);
                }
            }
            
            logger.warning("Nenhum resultado encontrado para: " + endereco);
            return Optional.empty();
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.warning("Erro de conexão com Nominatim: " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.warning("Erro ao geocodificar com Nominatim: " + e.getMessage());
            return Optional.empty();
        }
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
                
            } catch (org.springframework.web.reactive.function.client.WebClientException e) {
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
        String coordinates = String.format("%s,%s;%s,%s",
            origemLon, origemLat,
            destinoLon, destinoLat);
        
        String routeProfile = (profile != null && !profile.isEmpty()) ? profile : "driving";
        
        OsrmRouteResponse response = osrmClient.get()
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
        
        if (response == null || response.getCode() == null || !"Ok".equals(response.getCode())) {
            logger.warning("OSRM retornou código inválido: " + (response != null ? response.getCode() : "null"));
            return Optional.empty();
        }
        
        if (response.getRoutes() == null || response.getRoutes().isEmpty()) {
            logger.warning("OSRM não retornou rotas");
            return Optional.empty();
        }
        
        OsrmRoute route = response.getRoutes().get(0);
        
        double distanciaMetros = route.getDistance();
        BigDecimal distanciaKm = BigDecimal.valueOf(distanciaMetros / 1000.0)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        
        double tempoSegundos = route.getDuration();
        int tempoMinutos = (int) Math.ceil(tempoSegundos / 60.0);
        
        List<Coordinates> waypoints = null;
        if (includeWaypoints) {
            waypoints = extrairWaypoints(route);
        }
        
        RouteResult result = new RouteResult(distanciaKm, tempoMinutos, waypoints);
        logger.fine("Rota calculada via OSRM: " + distanciaKm + " km, " + tempoMinutos + " min" + 
                   (waypoints != null ? ", " + waypoints.size() + " waypoints" : ""));
        return Optional.of(result);
    }
    
    private List<Coordinates> extrairWaypoints(OsrmRoute route) {
        List<Coordinates> waypoints = new ArrayList<>();
        
        List<List<Double>> coordinates = route.getCoordinates();
        if (coordinates != null && !coordinates.isEmpty()) {
            for (List<Double> coord : coordinates) {
                if (coord != null && coord.size() >= 2) {
                    BigDecimal longitude = BigDecimal.valueOf(coord.get(0));
                    BigDecimal latitude = BigDecimal.valueOf(coord.get(1));
                    waypoints.add(new Coordinates(latitude, longitude));
                }
            }
        } else {
            String geometryStr = route.getGeometryAsString();
            if (geometryStr != null && !geometryStr.isEmpty()) {
                waypoints = PolylineDecoder.decode(geometryStr);
            }
        }
        
        return waypoints;
    }
    
    public String obterProfileOSRM(com.siseg.model.enumerations.TipoVeiculo tipoVeiculo) {
        return com.siseg.util.VehicleConstants.getOsrmProfile(tipoVeiculo);
    }
    
    public void clearCache() {
        cache.clear();
    }
}

