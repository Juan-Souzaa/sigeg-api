package com.siseg.service;

import com.siseg.dto.rastreamento.RastreamentoDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.DistanceCalculator;
import com.siseg.util.TempoEstimadoCalculator;
import com.siseg.util.VehicleConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.logging.Logger;

@Service
public class RastreamentoService {
    
    private static final Logger logger = Logger.getLogger(RastreamentoService.class.getName());
    private static final BigDecimal DISTANCIA_PROXIMO_DESTINO = new BigDecimal("0.1");
    
    private final PedidoRepository pedidoRepository;
    private final EntregadorRepository entregadorRepository;
    private final TempoEstimadoCalculator tempoEstimadoCalculator;
    
    public RastreamentoService(PedidoRepository pedidoRepository, EntregadorRepository entregadorRepository,
                               TempoEstimadoCalculator tempoEstimadoCalculator) {
        this.pedidoRepository = pedidoRepository;
        this.entregadorRepository = entregadorRepository;
        this.tempoEstimadoCalculator = tempoEstimadoCalculator;
    }
    
    @Transactional(readOnly = true)
    public RastreamentoDTO obterRastreamento(Long pedidoId) {
        Pedido pedido = buscarPedidoComEntregador(pedidoId);
        Entregador entregador = buscarEntregador(pedido.getEntregador().getId());
        
        RastreamentoDTO rastreamento = criarRastreamentoDTO(pedido, entregador);
        
        if (!temCoordenadasValidas(pedido, entregador)) {
            return rastreamento;
        }
        
        calcularDistanciaETempoRastreamento(rastreamento, entregador, pedido);
        
        return rastreamento;
    }
    
    private Pedido buscarPedidoComEntregador(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        if (pedido.getEntregador() == null) {
            throw new ResourceNotFoundException("Pedido não possui entregador associado");
        }
        
        return pedido;
    }
    
    private Entregador buscarEntregador(Long entregadorId) {
        return entregadorRepository.findById(entregadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado"));
    }
    
    private RastreamentoDTO criarRastreamentoDTO(Pedido pedido, Entregador entregador) {
        RastreamentoDTO rastreamento = new RastreamentoDTO();
        rastreamento.setStatusEntrega(pedido.getStatus());
        rastreamento.setPosicaoDestinoLat(pedido.getLatitudeEntrega());
        rastreamento.setPosicaoDestinoLon(pedido.getLongitudeEntrega());
        rastreamento.setPosicaoAtualLat(entregador.getLatitude());
        rastreamento.setPosicaoAtualLon(entregador.getLongitude());
        return rastreamento;
    }
    
    private boolean temCoordenadasValidas(Pedido pedido, Entregador entregador) {
        if (pedido.getLatitudeEntrega() == null || pedido.getLongitudeEntrega() == null) {
            logger.warning("Pedido sem coordenadas de destino para rastreamento");
            return false;
        }
        
        if (entregador.getLatitude() == null || entregador.getLongitude() == null) {
            logger.warning("Entregador sem coordenadas para rastreamento");
            return false;
        }
        
        return true;
    }
    
    private void calcularDistanciaETempoRastreamento(RastreamentoDTO rastreamento, Entregador entregador, Pedido pedido) {
        var resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
            entregador.getLatitude(),
            entregador.getLongitude(),
            pedido.getLatitudeEntrega(),
            pedido.getLongitudeEntrega(),
            entregador.getTipoVeiculo()
        );
        
        if (resultado.getDistanciaKm() != null && resultado.getDistanciaKm().compareTo(BigDecimal.ZERO) > 0) {
            rastreamento.setDistanciaRestanteKm(resultado.getDistanciaKm());
            rastreamento.setProximoAoDestino(resultado.getDistanciaKm().compareTo(DISTANCIA_PROXIMO_DESTINO) <= 0);
            rastreamento.setTempoEstimadoMinutos(resultado.getTempoMinutos());
        } else {
            rastreamento.setDistanciaRestanteKm(BigDecimal.ZERO);
            rastreamento.setProximoAoDestino(true);
            rastreamento.setTempoEstimadoMinutos(0);
        }
    }
    
    @Transactional
    public void simularMovimento(Long pedidoId) {
        Pedido pedido = buscarPedidoParaSimulacao(pedidoId);
        Entregador entregador = buscarEntregador(pedido.getEntregador().getId());
        
        var coordenadas = obterCoordenadasOrigem(entregador, pedido);
        if (coordenadas == null) {
            return;
        }
        
        BigDecimal distanciaParaDestino = calcularDistanciaParaDestino(coordenadas, pedido);
        
        if (devePosicionarNoDestino(distanciaParaDestino, entregador)) {
            posicionarNoDestino(entregador, pedido);
            return;
        }
        
        processarMovimento(entregador, coordenadas, pedido, distanciaParaDestino);
    }
    
    private BigDecimal calcularDistanciaParaDestino(CoordenadasOrigem origem, Pedido pedido) {
        return DistanceCalculator.calculateDistance(
            origem.origemLat(), origem.origemLon(),
            pedido.getLatitudeEntrega(), pedido.getLongitudeEntrega()
        );
    }
    
    private boolean devePosicionarNoDestino(BigDecimal distanciaParaDestino, Entregador entregador) {
        if (isProximoAoDestino(distanciaParaDestino)) {
            return true;
        }
        
        double velocidadeKmh = calcularVelocidade(entregador);
        double distanciaPorIteracaoKm = calcularDistanciaPorIteracao(velocidadeKmh);
        
        if (distanciaParaDestino != null && distanciaParaDestino.doubleValue() <= distanciaPorIteracaoKm) {
            logger.info("Entregador chegou ao destino");
            return true;
        }
        
        return false;
    }
    
    private void processarMovimento(Entregador entregador, CoordenadasOrigem coordenadas, 
                                    Pedido pedido, BigDecimal distanciaParaDestino) {
        double velocidadeKmh = calcularVelocidade(entregador);
        double distanciaPorIteracaoKm = calcularDistanciaPorIteracao(velocidadeKmh);
        moverEmDirecaoAoDestino(entregador, coordenadas, pedido, distanciaParaDestino, distanciaPorIteracaoKm, velocidadeKmh);
    }
    
    private Pedido buscarPedidoParaSimulacao(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        validarStatusEntrega(pedido);
        validarEntregadorAssociado(pedido);
        validarCoordenadasDestino(pedido);
        
        return pedido;
    }
    
    private void validarStatusEntrega(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.OUT_FOR_DELIVERY) {
            logger.fine("Pedido não está em entrega, ignorando simulação: " + pedido.getId());
            throw new IllegalStateException("Pedido não está em entrega");
        }
    }
    
    private void validarEntregadorAssociado(Pedido pedido) {
        if (pedido.getEntregador() == null) {
            logger.warning("Pedido sem entregador, ignorando simulação: " + pedido.getId());
            throw new IllegalStateException("Pedido sem entregador");
        }
    }
    
    private void validarCoordenadasDestino(Pedido pedido) {
        if (pedido.getLatitudeEntrega() == null || pedido.getLongitudeEntrega() == null) {
            logger.warning("Pedido sem coordenadas de destino, ignorando simulação: " + pedido.getId());
            throw new IllegalStateException("Pedido sem coordenadas de destino");
        }
    }
    
    private CoordenadasOrigem obterCoordenadasOrigem(Entregador entregador, Pedido pedido) {
        if (temCoordenadasEntregador(entregador)) {
            return new CoordenadasOrigem(entregador.getLatitude(), entregador.getLongitude());
        }
        
        return inicializarCoordenadasDoRestaurante(entregador, pedido);
    }
    
    private boolean temCoordenadasEntregador(Entregador entregador) {
        return entregador.getLatitude() != null && entregador.getLongitude() != null;
    }
    
    private CoordenadasOrigem inicializarCoordenadasDoRestaurante(Entregador entregador, Pedido pedido) {
        if (!temCoordenadasRestaurante(pedido)) {
            logger.warning("Não é possível inicializar posição do entregador: restaurante sem coordenadas");
            return null;
        }
        
        BigDecimal origemLat = pedido.getRestaurante().getLatitude();
        BigDecimal origemLon = pedido.getRestaurante().getLongitude();
        
        entregador.setLatitude(origemLat);
        entregador.setLongitude(origemLon);
        entregadorRepository.save(entregador);
        logger.info("Inicializada posição do entregador com coordenadas do restaurante");
        
        return new CoordenadasOrigem(origemLat, origemLon);
    }
    
    private boolean temCoordenadasRestaurante(Pedido pedido) {
        return pedido.getRestaurante() != null && 
               pedido.getRestaurante().getLatitude() != null && 
               pedido.getRestaurante().getLongitude() != null;
    }
    
    private boolean isProximoAoDestino(BigDecimal distancia) {
        return distancia == null || distancia.compareTo(DISTANCIA_PROXIMO_DESTINO) <= 0;
    }
    
    private void posicionarNoDestino(Entregador entregador, Pedido pedido) {
        entregador.setLatitude(pedido.getLatitudeEntrega());
        entregador.setLongitude(pedido.getLongitudeEntrega());
        entregadorRepository.save(entregador);
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
        return (velocidadeKmh / VehicleConstants.SEGUNDOS_POR_SEGUNDO) 
               * VehicleConstants.INTERVALO_SIMULACAO_SEGUNDOS;
    }
    
    private void moverEmDirecaoAoDestino(Entregador entregador, CoordenadasOrigem origem, 
                                         Pedido pedido, BigDecimal distanciaTotal, 
                                         double distanciaPorIteracaoKm, double velocidadeKmh) {
        double percentualProgresso = distanciaPorIteracaoKm / distanciaTotal.doubleValue();
        
        BigDecimal novaLat = origem.origemLat().add(
            pedido.getLatitudeEntrega().subtract(origem.origemLat())
                .multiply(BigDecimal.valueOf(percentualProgresso))
        );
        BigDecimal novaLon = origem.origemLon().add(
            pedido.getLongitudeEntrega().subtract(origem.origemLon())
                .multiply(BigDecimal.valueOf(percentualProgresso))
        );
        
        entregador.setLatitude(novaLat);
        entregador.setLongitude(novaLon);
        entregadorRepository.save(entregador);
        
        logger.info(String.format(
            "Simulação de movimento: Pedido %d - Entregador movido (Velocidade: %.1f km/h, Progresso: %.1f%%)",
            pedido.getId(), velocidadeKmh, percentualProgresso * 100
        ));
    }
    
    private record CoordenadasOrigem(BigDecimal origemLat, BigDecimal origemLon) {}
}

