package com.siseg.service;

import com.siseg.dto.rastreamento.RastreamentoDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.logging.Logger;

@Service
public class RastreamentoService {
    
    private static final Logger logger = Logger.getLogger(RastreamentoService.class.getName());
    private static final BigDecimal DISTANCIA_PROXIMO_DESTINO = new BigDecimal("0.1"); // 100 metros
    
    private final PedidoRepository pedidoRepository;
    private final EntregadorRepository entregadorRepository;
    private final GeocodingService geocodingService;
    
    public RastreamentoService(PedidoRepository pedidoRepository, EntregadorRepository entregadorRepository,
                               GeocodingService geocodingService) {
        this.pedidoRepository = pedidoRepository;
        this.entregadorRepository = entregadorRepository;
        this.geocodingService = geocodingService;
    }
    
    @Transactional(readOnly = true)
    public RastreamentoDTO obterRastreamento(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        if (pedido.getEntregador() == null) {
            throw new ResourceNotFoundException("Pedido não possui entregador associado");
        }
        
        Entregador entregador = pedido.getEntregador();
        
        RastreamentoDTO rastreamento = new RastreamentoDTO();
        rastreamento.setStatusEntrega(pedido.getStatus());
        
        if (pedido.getLatitudeEntrega() == null || pedido.getLongitudeEntrega() == null) {
            logger.warning("Pedido sem coordenadas de destino para rastreamento");
            return rastreamento;
        }
        
        rastreamento.setPosicaoDestinoLat(pedido.getLatitudeEntrega());
        rastreamento.setPosicaoDestinoLon(pedido.getLongitudeEntrega());
        
        if (entregador.getLatitude() == null || entregador.getLongitude() == null) {
            logger.warning("Entregador sem coordenadas para rastreamento");
            rastreamento.setPosicaoAtualLat(null);
            rastreamento.setPosicaoAtualLon(null);
            return rastreamento;
        }
        
        rastreamento.setPosicaoAtualLat(entregador.getLatitude());
        rastreamento.setPosicaoAtualLon(entregador.getLongitude());
        
        BigDecimal distanciaRestante = null;
        int tempoEstimado = 0;
        
        // Tentar calcular usando OSRM (rota real)
        try {
            String profile = "driving";
            if (entregador.getTipoVeiculo() != null) {
                String tipoVeiculo = entregador.getTipoVeiculo().name();
                if ("BICICLETA".equalsIgnoreCase(tipoVeiculo)) {
                    profile = "cycling";
                }
            }
            
            java.util.Optional<GeocodingService.RouteResult> routeResult = 
                geocodingService.calculateRoute(
                    entregador.getLatitude(),
                    entregador.getLongitude(),
                    pedido.getLatitudeEntrega(),
                    pedido.getLongitudeEntrega(),
                    profile
                );
            
            if (routeResult.isPresent()) {
                distanciaRestante = routeResult.get().getDistanciaKm();
                tempoEstimado = routeResult.get().getTempoMinutos();
                logger.info("Rastreamento - Pedido ID " + pedidoId + ": Distância calculada via OSRM (profile=" + profile + ") = " + distanciaRestante + " km, Tempo = " + tempoEstimado + " min");
            }
        } catch (Exception e) {
            logger.fine("Erro ao calcular rota via OSRM, usando fallback Haversine: " + e.getMessage());
        }
        
        // Fallback para Haversine se OSRM falhar ou não retornar resultado
        if (distanciaRestante == null) {
            distanciaRestante = DistanceCalculator.calculateDistance(
                entregador.getLatitude(),
                entregador.getLongitude(),
                pedido.getLatitudeEntrega(),
                pedido.getLongitudeEntrega()
            );
            
            if (distanciaRestante != null && distanciaRestante.compareTo(BigDecimal.ZERO) > 0) {
                tempoEstimado = DistanceCalculator.estimateDeliveryTime(
                    distanciaRestante,
                    entregador.getTipoVeiculo() != null ? entregador.getTipoVeiculo().name() : "MOTO"
                );
                logger.info("Rastreamento - Pedido ID " + pedidoId + ": Distância calculada via Haversine = " + distanciaRestante + " km, Tempo = " + tempoEstimado + " min");
            }
        }
        
        if (distanciaRestante != null && distanciaRestante.compareTo(BigDecimal.ZERO) > 0) {
            rastreamento.setDistanciaRestanteKm(distanciaRestante);
            rastreamento.setProximoAoDestino(distanciaRestante.compareTo(DISTANCIA_PROXIMO_DESTINO) <= 0);
            rastreamento.setTempoEstimadoMinutos(tempoEstimado);
        } else {
            rastreamento.setDistanciaRestanteKm(BigDecimal.ZERO);
            rastreamento.setProximoAoDestino(true);
            rastreamento.setTempoEstimadoMinutos(0);
        }
        
        return rastreamento;
    }
    
    @Transactional
    public void simularMovimento(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        if (pedido.getStatus() != StatusPedido.OUT_FOR_DELIVERY) {
            logger.fine("Pedido não está em entrega, ignorando simulação: " + pedidoId);
            return;
        }
        
        if (pedido.getEntregador() == null) {
            logger.warning("Pedido sem entregador, ignorando simulação: " + pedidoId);
            return;
        }
        
        if (pedido.getLatitudeEntrega() == null || pedido.getLongitudeEntrega() == null) {
            logger.warning("Pedido sem coordenadas de destino, ignorando simulação: " + pedidoId);
            return;
        }
        
        Entregador entregador = pedido.getEntregador();
        
        BigDecimal destinoLat = pedido.getLatitudeEntrega();
        BigDecimal destinoLon = pedido.getLongitudeEntrega();
        
        BigDecimal origemLat;
        BigDecimal origemLon;
        
        if (entregador.getLatitude() == null || entregador.getLongitude() == null) {
            if (pedido.getRestaurante() != null && 
                pedido.getRestaurante().getLatitude() != null && 
                pedido.getRestaurante().getLongitude() != null) {
                origemLat = pedido.getRestaurante().getLatitude();
                origemLon = pedido.getRestaurante().getLongitude();
                entregador.setLatitude(origemLat);
                entregador.setLongitude(origemLon);
                entregadorRepository.save(entregador);
                logger.info("Inicializada posição do entregador com coordenadas do restaurante");
            } else {
                logger.warning("Não é possível inicializar posição do entregador: restaurante sem coordenadas");
                return;
            }
        } else {
            origemLat = entregador.getLatitude();
            origemLon = entregador.getLongitude();
        }
        
        BigDecimal distanciaAtual = DistanceCalculator.calculateDistance(
            origemLat, origemLon, destinoLat, destinoLon
        );
        
        if (distanciaAtual == null || distanciaAtual.compareTo(DISTANCIA_PROXIMO_DESTINO) <= 0) {
            logger.fine("Entregador já está próximo ao destino (< 100m), não movendo");
            return;
        }
        
        // Velocidade base por tipo de veículo
        double velocidadeBaseKmh = entregador.getTipoVeiculo().name().equals("BICICLETA") ? 15.0 : 30.0;
        
        // Variação aleatória de velocidade (±20% para simular trânsito, semáforos, etc)
        double variacao = (Math.random() * 0.4) - 0.2; // -0.2 a +0.2 (20% para mais ou menos)
        double velocidadeKmh = velocidadeBaseKmh * (1.0 + variacao);
        
        // Garantir velocidade mínima (não pode ser negativa ou muito baixa)
        if (velocidadeKmh < velocidadeBaseKmh * 0.5) {
            velocidadeKmh = velocidadeBaseKmh * 0.5; // Mínimo 50% da velocidade base
        }
        if (velocidadeKmh > velocidadeBaseKmh * 1.5) {
            velocidadeKmh = velocidadeBaseKmh * 1.5; // Máximo 150% da velocidade base
        }
        
        double distanciaPorIteracao = (velocidadeKmh / 3600.0) * 10.0;
        
        if (distanciaAtual.doubleValue() <= distanciaPorIteracao) {
            entregador.setLatitude(destinoLat);
            entregador.setLongitude(destinoLon);
            entregadorRepository.save(entregador);
            logger.info("Entregador chegou ao destino");
            return;
        }
        
        double percentualProgresso = distanciaPorIteracao / distanciaAtual.doubleValue();
        
        BigDecimal novaLat = origemLat.add(
            destinoLat.subtract(origemLat).multiply(BigDecimal.valueOf(percentualProgresso))
        );
        BigDecimal novaLon = origemLon.add(
            destinoLon.subtract(origemLon).multiply(BigDecimal.valueOf(percentualProgresso))
        );
        
        entregador.setLatitude(novaLat);
        entregador.setLongitude(novaLon);
        entregadorRepository.save(entregador);
        
        BigDecimal novaDistancia = DistanceCalculator.calculateDistance(
            novaLat, novaLon, destinoLat, destinoLon
        );
        
        logger.info(String.format(
            "Simulação de movimento: Pedido %d - Distância restante: %.2f km -> %.2f km (Velocidade: %.1f km/h)",
            pedidoId, distanciaAtual.doubleValue(), 
            novaDistancia != null ? novaDistancia.doubleValue() : 0.0,
            velocidadeKmh
        ));
    }
}

