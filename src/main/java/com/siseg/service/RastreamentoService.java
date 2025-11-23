package com.siseg.service;

import com.siseg.dto.rastreamento.RastreamentoDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Endereco;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.service.EnderecoService;
import com.siseg.service.RouteService;
import com.siseg.util.TempoEstimadoCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class RastreamentoService {
    
    private static final Logger logger = Logger.getLogger(RastreamentoService.class.getName());
    private static final BigDecimal DISTANCIA_PROXIMO_DESTINO = new BigDecimal("0.1");
    
    private final PedidoRepository pedidoRepository;
    private final EntregadorRepository entregadorRepository;
    private final TempoEstimadoCalculator tempoEstimadoCalculator;
    private final EnderecoService enderecoService;
    private final RouteService routeService;
    
    public RastreamentoService(PedidoRepository pedidoRepository, EntregadorRepository entregadorRepository,
                               TempoEstimadoCalculator tempoEstimadoCalculator, EnderecoService enderecoService,
                               RouteService routeService) {
        this.pedidoRepository = pedidoRepository;
        this.entregadorRepository = entregadorRepository;
        this.tempoEstimadoCalculator = tempoEstimadoCalculator;
        this.enderecoService = enderecoService;
        this.routeService = routeService;
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
        
        if (pedido.getEnderecoEntrega() != null) {
            rastreamento.setPosicaoDestinoLat(pedido.getEnderecoEntrega().getLatitude());
            rastreamento.setPosicaoDestinoLon(pedido.getEnderecoEntrega().getLongitude());
        }
        
        
        if (pedido.getRestaurante() != null) {
            Optional<Endereco> enderecoPrincipal = enderecoService.buscarEnderecoPrincipalRestaurante(pedido.getRestaurante().getId());
            if (enderecoPrincipal.isPresent()) {
                Endereco endereco = enderecoPrincipal.get();
                if (endereco.getLatitude() != null && endereco.getLongitude() != null) {
                    rastreamento.setPosicaoRestauranteLat(endereco.getLatitude());
                    rastreamento.setPosicaoRestauranteLon(endereco.getLongitude());
                }
            }
        }
        
        rastreamento.setPosicaoAtualLat(entregador.getLatitude());
        rastreamento.setPosicaoAtualLon(entregador.getLongitude());
        rastreamento.setDistanciaRestanteKm(BigDecimal.ZERO);
        rastreamento.setTempoEstimadoMinutos(0);
        rastreamento.setProximoAoDestino(true);
        
        var waypointsRestantes = routeService.obterWaypointsRestantes(pedido.getId());
        if (!waypointsRestantes.isEmpty()) {
            rastreamento.setWaypoints(waypointsRestantes);
        }
        
        return rastreamento;
    }
    
    private boolean temCoordenadasValidas(Pedido pedido, Entregador entregador) {
        if (pedido.getEnderecoEntrega() == null || 
            pedido.getEnderecoEntrega().getLatitude() == null || 
            pedido.getEnderecoEntrega().getLongitude() == null) {
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
            pedido.getEnderecoEntrega().getLatitude(),
            pedido.getEnderecoEntrega().getLongitude(),
            entregador.getTipoVeiculo()
        );
        
        if (resultado.getDistanciaKm() != null && resultado.getDistanciaKm().compareTo(BigDecimal.ZERO) > 0) {
            rastreamento.setDistanciaRestanteKm(resultado.getDistanciaKm());
            rastreamento.setProximoAoDestino(resultado.getDistanciaKm().compareTo(DISTANCIA_PROXIMO_DESTINO) <= 0);
            rastreamento.setTempoEstimadoMinutos(resultado.getTempoMinutos());
        }
    }
}

