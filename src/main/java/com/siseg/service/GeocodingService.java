package com.siseg.service;

import com.siseg.dto.geocoding.Coordinates;
import com.siseg.dto.geocoding.NominatimResponse;
import com.siseg.dto.geocoding.OsrmRoute;
import com.siseg.dto.geocoding.OsrmRouteResponse;
import com.siseg.dto.geocoding.RouteResult;
import com.siseg.dto.geocoding.ViaCepResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Service
public class GeocodingService {
    
    private static final Logger logger = Logger.getLogger(GeocodingService.class.getName());
    
    private final WebClient nominatimClient;
    private final WebClient viacepClient;
    private final WebClient osrmClient;
    private final Map<String, Coordinates> cache = new ConcurrentHashMap<>();
    
    private volatile long lastRequestTime = 0;
    
    public GeocodingService(@Value("${geocoding.nominatim.baseUrl}") String nominatimBaseUrl,
                           @Value("${geocoding.viacep.baseUrl}") String viacepBaseUrl,
                           @Value("${geocoding.osrm.baseUrl}") String osrmBaseUrl,
                           @Value("${geocoding.nominatim.userAgent}") String nominatimUserAgent) {
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
                .build();
    }
    
    /**
     * Geocodifica um endereço ou CEP, retornando coordenadas (latitude, longitude).
     * Se for CEP, primeiro busca o endereço completo via ViaCEP.
     * 
     * @param enderecoOuCep Endereço completo ou CEP (formato: 12345678 ou 12345-678)
     * @return Optional com coordenadas, ou empty se não conseguir geocodificar
     */
    public Optional<Coordinates> geocodeAddress(String enderecoOuCep) {
        if (enderecoOuCep == null || enderecoOuCep.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String normalized = enderecoOuCep.trim();
        
        // Verificar cache
        if (cache.containsKey(normalized)) {
            logger.fine("Coordenadas encontradas no cache para: " + normalized);
            return Optional.of(cache.get(normalized));
        }
        
        try {
            String enderecoCompleto = normalized;
            
            // Se for CEP, buscar endereço completo via ViaCEP
            if (isCep(normalized)) {
                Optional<String> enderecoViaCep = buscarEnderecoPorCep(normalized);
                if (enderecoViaCep.isPresent()) {
                    enderecoCompleto = enderecoViaCep.get();
                    logger.info("Endereço encontrado via CEP: " + enderecoCompleto);
                } else {
                    logger.warning("Não foi possível buscar endereço para CEP: " + normalized);
                    return Optional.empty();
                }
            }
            
            // Geocodificar endereço completo com Nominatim
            Optional<Coordinates> coordenadas = geocodeWithNominatim(enderecoCompleto);
            
            // Cachear resultado se bem-sucedido
            if (coordenadas.isPresent()) {
                cache.put(normalized, coordenadas.get());
                // Também cachear o endereço completo se diferente
                if (!enderecoCompleto.equals(normalized)) {
                    cache.put(enderecoCompleto, coordenadas.get());
                }
            }
            
            return coordenadas;
            
        } catch (Exception e) {
            logger.warning("Erro ao geocodificar endereço '" + normalized + "': " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Verifica se o input é um CEP (formato: 12345678 ou 12345-678)
     */
    private boolean isCep(String input) {
        if (input == null) return false;
        // Remove espaços e verifica se é apenas números com ou sem hífen
        String cleaned = input.replaceAll("\\s+", "").replace("-", "");
        return cleaned.matches("^\\d{8}$");
    }
    
    /**
     * Busca endereço completo via ViaCEP API
     */
    private Optional<String> buscarEnderecoPorCep(String cep) {
        try {
            // Remove hífen e espaços do CEP
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
                // Montar endereço completo: "logradouro, bairro, localidade, uf"
                StringBuilder endereco = new StringBuilder();
                endereco.append(response.getLogradouro());
                
                if (response.getBairro() != null && !response.getBairro().isEmpty()) {
                    endereco.append(", ").append(response.getBairro());
                }
                
                if (response.getLocalidade() != null && !response.getLocalidade().isEmpty()) {
                    endereco.append(", ").append(response.getLocalidade());
                }
                
                if (response.getUf() != null && !response.getUf().isEmpty()) {
                    endereco.append(", ").append(response.getUf());
                }
                
                // Adicionar Brasil para melhorar precisão da geocodificação
                endereco.append(", Brasil");
                
                return Optional.of(endereco.toString());
            } else {
                logger.warning("CEP não encontrado ou erro na resposta ViaCEP: " + cep);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.warning("Erro ao buscar CEP no ViaCEP: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Geocodifica endereço usando Nominatim API
     */
    private Optional<Coordinates> geocodeWithNominatim(String endereco) {
        try {
            // Respeitar rate limit: 1 requisição por segundo
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
    
    /**
     * Respeita rate limit do Nominatim (1 requisição por segundo)
     */
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
    
    /**
     * Calcula distância e tempo de viagem usando OSRM (Open Source Routing Machine)
     * Retorna distância real de rota e tempo estimado
     * 
     * @param origemLat Latitude do ponto de origem
     * @param origemLon Longitude do ponto de origem
     * @param destinoLat Latitude do ponto de destino
     * @param destinoLon Longitude do ponto de destino
     * @param profile Perfil de roteamento: "driving", "walking", "cycling" (padrão: "driving")
     * @return Optional com RouteResult contendo distância (km) e tempo (minutos), ou empty se falhar
     */
    public Optional<RouteResult> calculateRoute(BigDecimal origemLat, BigDecimal origemLon,
                                               BigDecimal destinoLat, BigDecimal destinoLon,
                                               String profile) {
        if (origemLat == null || origemLon == null || destinoLat == null || destinoLon == null) {
            return Optional.empty();
        }
        
        try {
            // Formato OSRM: longitude,latitude;longitude,latitude
            String coordinates = String.format("%s,%s;%s,%s",
                origemLon, origemLat,
                destinoLon, destinoLat);
            
            String routeProfile = (profile != null && !profile.isEmpty()) ? profile : "driving";
            
            OsrmRouteResponse response = osrmClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/{profile}/{coordinates}")
                            .queryParam("overview", "false")
                            .queryParam("alternatives", "false")
                            .queryParam("steps", "false")
                            .build(routeProfile, coordinates))
                    .retrieve()
                    .bodyToMono(OsrmRouteResponse.class)
                    .block();
            
            if (response != null && response.getCode() != null && 
                "Ok".equals(response.getCode()) && 
                response.getRoutes() != null && 
                !response.getRoutes().isEmpty()) {
                
                OsrmRoute route = response.getRoutes().get(0);
                
                // distance está em metros, converter para km
                double distanciaMetros = route.getDistance();
                BigDecimal distanciaKm = BigDecimal.valueOf(distanciaMetros / 1000.0)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                
                // duration está em segundos, converter para minutos
                double tempoSegundos = route.getDuration();
                int tempoMinutos = (int) Math.ceil(tempoSegundos / 60.0);
                
                RouteResult result = new RouteResult(distanciaKm, tempoMinutos);
                logger.fine("Rota calculada via OSRM: " + distanciaKm + " km, " + tempoMinutos + " min");
                return Optional.of(result);
            }
            
            logger.warning("OSRM não retornou rota válida");
            return Optional.empty();
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.warning("Erro de conexão com OSRM: " + e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            logger.warning("Erro ao calcular rota com OSRM: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Obtém o profile OSRM apropriado baseado no tipo de veículo
     * @param tipoVeiculo Tipo de veículo do entregador
     * @return Profile OSRM: "cycling" para bicicleta, "driving" para outros
     */
    public String obterProfileOSRM(com.siseg.model.enumerations.TipoVeiculo tipoVeiculo) {
        return com.siseg.util.VehicleConstants.getOsrmProfile(tipoVeiculo);
    }
    
    /**
     * Limpa o cache (útil para testes ou quando necessário)
     */
    public void clearCache() {
        cache.clear();
    }
}

