package com.siseg.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siseg.dto.geocoding.Coordinates;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.RotaEntrega;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.DistanceCalculator;
import com.siseg.util.VehicleConstants;
import com.siseg.validator.PedidoValidator;

@Service
public class DeliveryMovementService {
    
    private static final Logger logger = Logger.getLogger(DeliveryMovementService.class.getName());
    private static final BigDecimal DISTANCIA_PROXIMO_DESTINO = new BigDecimal("0.01");
    
    private final PedidoRepository pedidoRepository;
    private final EntregadorRepository entregadorRepository;
    private final RouteService routeService;
    private final PedidoValidator pedidoValidator;
    
    public DeliveryMovementService(PedidoRepository pedidoRepository,
                                  EntregadorRepository entregadorRepository,
                                  RouteService routeService,
                                  PedidoValidator pedidoValidator) {
        this.pedidoRepository = pedidoRepository;
        this.entregadorRepository = entregadorRepository;
        this.routeService = routeService;
        this.pedidoValidator = pedidoValidator;
    }
    
    @Transactional
    public void simularMovimento(Long pedidoId) {
        Pedido pedido = buscarPedidoParaSimulacao(pedidoId);
        Entregador entregador = buscarEntregador(pedido.getEntregador().getId());
        
        inicializarRotaSeNecessario(pedido, entregador);
        
        if (verificarChegadaAoDestino(pedido, entregador)) {
            posicionarNoDestino(entregador, pedido);
            return;
        }
        
        moverParaProximoWaypoint(entregador, pedido);
    }
    
    private Pedido buscarPedidoParaSimulacao(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        pedidoValidator.validateStatusEntrega(pedido);
        pedidoValidator.validateEntregadorAssociado(pedido);
        pedidoValidator.validateCoordenadasDestino(pedido);
        
        return pedido;
    }
    
    private Entregador buscarEntregador(Long entregadorId) {
        return entregadorRepository.findById(entregadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado"));
    }
    
    private void inicializarRotaSeNecessario(Pedido pedido, Entregador entregador) {
        Optional<RotaEntrega> rotaExistente = routeService.obterRota(pedido.getId());
        
        if (rotaExistente.isEmpty()) {
            logger.info("Calculando rota para pedido " + pedido.getId());
            
            if (pedido.getRestaurante() != null) {
                var enderecoRestaurante = pedido.getRestaurante().getEnderecoPrincipal();
                if (enderecoRestaurante.isPresent() && 
                    enderecoRestaurante.get().getLatitude() != null && 
                    enderecoRestaurante.get().getLongitude() != null) {
                    entregador.setLatitude(enderecoRestaurante.get().getLatitude());
                    entregador.setLongitude(enderecoRestaurante.get().getLongitude());
                    entregadorRepository.save(entregador);
                    logger.info("Posição do entregador definida com coordenadas do restaurante para iniciar entrega");
                } else {
                    throw new IllegalStateException("Não é possível calcular rota: restaurante sem coordenadas");
                }
            } else {
                throw new IllegalStateException("Não é possível calcular rota: restaurante não encontrado");
            }
            
            routeService.calcularERegistrarRota(pedido, entregador);
        }
    }
    
    private void moverParaProximoWaypoint(Entregador entregador, Pedido pedido) {
        List<Coordinates> waypointsRestantes = routeService.obterWaypointsRestantes(pedido.getId());
        
        if (waypointsRestantes.isEmpty()) {
            posicionarNoDestino(entregador, pedido);
            return;
        }
        
        double velocidadeKmh = calcularVelocidade(entregador);
        double distanciaPorIteracaoKm = calcularDistanciaPorIteracao(velocidadeKmh);
        
        BigDecimal posicaoAtualLat = entregador.getLatitude();
        BigDecimal posicaoAtualLon = entregador.getLongitude();
        BigDecimal distanciaRestante = BigDecimal.valueOf(distanciaPorIteracaoKm);
        
        int waypointsAvancados = 0;
        
        for (int i = 0; i < waypointsRestantes.size() && distanciaRestante.compareTo(BigDecimal.ZERO) > 0; i++) {
            Coordinates waypoint = waypointsRestantes.get(i);
            BigDecimal distanciaAteWaypoint = calcularDistancia(
                posicaoAtualLat, posicaoAtualLon,
                waypoint.getLatitude(), waypoint.getLongitude()
            );
            
            if (distanciaAteWaypoint == null || distanciaAteWaypoint.compareTo(BigDecimal.ZERO) <= 0) {
                posicaoAtualLat = waypoint.getLatitude();
                posicaoAtualLon = waypoint.getLongitude();
                waypointsAvancados++;
                distanciaRestante = distanciaRestante.subtract(distanciaAteWaypoint != null ? distanciaAteWaypoint : BigDecimal.ZERO);
            } else if (distanciaAteWaypoint.compareTo(distanciaRestante) <= 0) {
                posicaoAtualLat = waypoint.getLatitude();
                posicaoAtualLon = waypoint.getLongitude();
                waypointsAvancados++;
                distanciaRestante = distanciaRestante.subtract(distanciaAteWaypoint);
            } else {
                double percentualProgresso = distanciaRestante.doubleValue() / distanciaAteWaypoint.doubleValue();
                posicaoAtualLat = posicaoAtualLat.add(
                    waypoint.getLatitude().subtract(posicaoAtualLat)
                        .multiply(BigDecimal.valueOf(percentualProgresso))
                );
                posicaoAtualLon = posicaoAtualLon.add(
                    waypoint.getLongitude().subtract(posicaoAtualLon)
                        .multiply(BigDecimal.valueOf(percentualProgresso))
                );
                distanciaRestante = BigDecimal.ZERO;
                break;
            }
        }
        
        entregador.setLatitude(posicaoAtualLat);
        entregador.setLongitude(posicaoAtualLon);
        entregadorRepository.save(entregador);
        
        // Avançar waypoints se necessário
        if (waypointsAvancados > 0) {
            routeService.avancarWaypoints(pedido.getId(), waypointsAvancados);
            logger.info(String.format(
                "Entregador avançou %d waypoint(s) do pedido %d (Velocidade: %.1f km/h, Distância percorrida: %.4f km)",
                waypointsAvancados, pedido.getId(), velocidadeKmh, distanciaPorIteracaoKm
            ));
        } else {
            logger.info(String.format(
                "Simulação de movimento - Pedido %d: Entregador movido parcialmente (Velocidade: %.1f km/h, Distância percorrida: %.4f km)",
                pedido.getId(), velocidadeKmh, distanciaPorIteracaoKm
            ));
        }
    }
    
    
    private boolean verificarChegadaAoDestino(Pedido pedido, Entregador entregador) {
        if (routeService.isRotaCompleta(pedido.getId()) && pedido.getEnderecoEntrega() != null) {
            BigDecimal distancia = calcularDistancia(
                entregador.getLatitude(), entregador.getLongitude(),
                pedido.getEnderecoEntrega().getLatitude(), pedido.getEnderecoEntrega().getLongitude()
            );
            
            return distancia != null && distancia.compareTo(DISTANCIA_PROXIMO_DESTINO) <= 0;
        }
        
        return false;
    }
    
    private void posicionarNoDestino(Entregador entregador, Pedido pedido) {
        if (pedido.getEnderecoEntrega() != null) {
            entregador.setLatitude(pedido.getEnderecoEntrega().getLatitude());
            entregador.setLongitude(pedido.getEnderecoEntrega().getLongitude());
            entregadorRepository.save(entregador);
            logger.info("Entregador chegou ao destino do pedido " + pedido.getId());
        }
    }
    
    private double calcularVelocidade(Entregador entregador) {
        double velocidadeBaseKmh = VehicleConstants.getVelocidadeMediaKmh(entregador.getTipoVeiculo());
        
        double variacao = (Math.random() * VehicleConstants.FATOR_VARIACAO_VELOCIDADE) 
                         - VehicleConstants.FATOR_DESVIO_VELOCIDADE;
        double velocidadeKmh = velocidadeBaseKmh * (1.0 + variacao);
        
        double velocidadeMinima = velocidadeBaseKmh * VehicleConstants.FATOR_VELOCIDADE_MINIMA;
        double velocidadeMaxima = velocidadeBaseKmh * VehicleConstants.FATOR_VELOCIDADE_MAXIMA;
        
        return Math.max(velocidadeMinima, Math.min(velocidadeMaxima, velocidadeKmh));
    }
    
    private double calcularDistanciaPorIteracao(double velocidadeKmh) {
        double distanciaBase = (velocidadeKmh / VehicleConstants.SEGUNDOS_POR_SEGUNDO) 
                               * VehicleConstants.INTERVALO_SIMULACAO_SEGUNDOS;
        return distanciaBase * VehicleConstants.FATOR_ACELERACAO_SIMULACAO;
    }
    
    private BigDecimal calcularDistancia(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        BigDecimal distancia = DistanceCalculator.calculateDistance(lat1, lon1, lat2, lon2);
        if (distancia != null) {
            return distancia.setScale(4, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }
}

