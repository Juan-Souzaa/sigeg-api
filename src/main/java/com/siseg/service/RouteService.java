package com.siseg.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.geocoding.Coordinates;
import com.siseg.dto.geocoding.RouteResult;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.RotaEntrega;
import com.siseg.repository.RotaEntregaRepository;

@Service
public class RouteService {
    
    private static final Logger logger = Logger.getLogger(RouteService.class.getName());
    
    private final GeocodingService geocodingService;
    private final RotaEntregaRepository rotaEntregaRepository;
    private final ObjectMapper objectMapper;
    
    public RouteService(GeocodingService geocodingService,
                       RotaEntregaRepository rotaEntregaRepository,
                       ObjectMapper objectMapper) {
        this.geocodingService = geocodingService;
        this.rotaEntregaRepository = rotaEntregaRepository;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public RotaEntrega calcularERegistrarRota(Pedido pedido, Entregador entregador) {
        Optional<RotaEntrega> rotaExistente = rotaEntregaRepository.findByPedidoId(pedido.getId());
        if (rotaExistente.isPresent()) {
            logger.info("Rota já existe para pedido " + pedido.getId());
            return rotaExistente.get();
        }
        
        BigDecimal origemLat = obterLatitudeOrigem(pedido, entregador);
        BigDecimal origemLon = obterLongitudeOrigem(pedido, entregador);
        
        if (origemLat == null || origemLon == null) {
            throw new IllegalStateException("Não foi possível determinar coordenadas de origem para calcular rota");
        }
        
        String profile = geocodingService.obterProfileOSRM(entregador.getTipoVeiculo());
        
        if (pedido.getEnderecoEntrega() == null || 
            pedido.getEnderecoEntrega().getLatitude() == null || 
            pedido.getEnderecoEntrega().getLongitude() == null) {
            throw new IllegalStateException("Pedido sem coordenadas de destino para calcular rota");
        }
        
        Optional<RouteResult> routeResult = geocodingService.calculateRoute(
            origemLat, origemLon,
            pedido.getEnderecoEntrega().getLatitude(), pedido.getEnderecoEntrega().getLongitude(),
            profile, true
        );
        
        if (routeResult.isEmpty() || routeResult.get().getWaypoints() == null || routeResult.get().getWaypoints().isEmpty()) {
            throw new IllegalStateException("Não foi possível calcular rota com waypoints para o pedido " + pedido.getId());
        }
        
        RotaEntrega rota = new RotaEntrega();
        rota.setPedido(pedido);
        rota.setIndiceAtual(0);
        
        try {
            String waypointsJson = objectMapper.writeValueAsString(routeResult.get().getWaypoints());
            rota.setWaypointsJson(waypointsJson);
        } catch (JsonProcessingException e) {
            logger.severe("Erro ao serializar waypoints para JSON: " + e.getMessage());
            throw new IllegalStateException("Erro ao salvar waypoints da rota", e);
        }
        
        RotaEntrega saved = rotaEntregaRepository.save(rota);
        logger.info("Rota calculada e registrada para pedido " + pedido.getId() + " com " + 
                   routeResult.get().getWaypoints().size() + " waypoints");
        
        return saved;
    }
    
    @Transactional(readOnly = true)
    public Optional<RotaEntrega> obterRota(Long pedidoId) {
        return rotaEntregaRepository.findByPedidoId(pedidoId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Coordinates> obterProximoWaypoint(Long pedidoId) {
        RotaEntrega rota = buscarRotaPorPedidoId(pedidoId);
        List<Coordinates> waypoints = deserializarWaypoints(rota);
        
        if (waypoints == null || waypoints.isEmpty()) {
            return Optional.empty();
        }
        
        int indiceAtual = rota.getIndiceAtual();
        if (indiceAtual >= waypoints.size()) {
            return Optional.empty();
        }
        
        return Optional.of(waypoints.get(indiceAtual));
    }
    
    @Transactional
    public void avancarWaypoint(Long pedidoId) {
        avancarWaypoints(pedidoId, 1);
    }
    
    @Transactional
    public void avancarWaypoints(Long pedidoId, int quantidade) {
        RotaEntrega rota = buscarRotaPorPedidoId(pedidoId);
        int indiceAtual = rota.getIndiceAtual();
        List<Coordinates> waypoints = deserializarWaypoints(rota);
        
        if (waypoints != null && indiceAtual < waypoints.size() - 1) {
            int novoIndice = Math.min(indiceAtual + quantidade, waypoints.size() - 1);
            rota.setIndiceAtual(novoIndice);
            rotaEntregaRepository.save(rota);
            logger.fine("Waypoints avançados para pedido " + pedidoId + ": " + novoIndice + "/" + waypoints.size());
        }
    }
    
    @Transactional(readOnly = true)
    public List<Coordinates> obterWaypointsRestantes(Long pedidoId) {
        RotaEntrega rota = buscarRotaPorPedidoId(pedidoId);
        List<Coordinates> waypoints = deserializarWaypoints(rota);
        
        if (waypoints == null || waypoints.isEmpty()) {
            return List.of();
        }
        
        int indiceAtual = rota.getIndiceAtual();
        if (indiceAtual >= waypoints.size()) {
            return List.of();
        }
        
        return waypoints.subList(indiceAtual, waypoints.size());
    }
    
    @Transactional(readOnly = true)
    public boolean isRotaCompleta(Long pedidoId) {
        RotaEntrega rota = buscarRotaPorPedidoId(pedidoId);
        List<Coordinates> waypoints = deserializarWaypoints(rota);
        
        if (waypoints == null || waypoints.isEmpty()) {
            return true;
        }
        
        return rota.getIndiceAtual() >= waypoints.size() - 1;
    }
    
    private RotaEntrega buscarRotaPorPedidoId(Long pedidoId) {
        return rotaEntregaRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Rota não encontrada para pedido: " + pedidoId));
    }
    
    private BigDecimal obterLatitudeOrigem(Pedido pedido, Entregador entregador) {
        if (pedido.getRestaurante() != null) {
            var enderecoRestaurante = pedido.getRestaurante().getEnderecoPrincipal();
            if (enderecoRestaurante.isPresent() && enderecoRestaurante.get().getLatitude() != null) {
                return enderecoRestaurante.get().getLatitude();
            }
        }
        return null;
    }
    
    private BigDecimal obterLongitudeOrigem(Pedido pedido, Entregador entregador) {
        if (pedido.getRestaurante() != null) {
            var enderecoRestaurante = pedido.getRestaurante().getEnderecoPrincipal();
            if (enderecoRestaurante.isPresent() && enderecoRestaurante.get().getLongitude() != null) {
                return enderecoRestaurante.get().getLongitude();
            }
        }
        return null;
    }
    
    @Transactional(readOnly = true)
    public List<Coordinates> deserializarWaypoints(RotaEntrega rota) {
        if (rota.getWaypointsJson() == null || rota.getWaypointsJson().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(rota.getWaypointsJson(), new TypeReference<List<Coordinates>>() {});
        } catch (JsonProcessingException e) {
            logger.warning("Erro ao deserializar waypoints do JSON: " + e.getMessage());
            return null;
        }
    }
}

