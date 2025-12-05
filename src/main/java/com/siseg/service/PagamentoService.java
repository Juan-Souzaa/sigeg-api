package com.siseg.service;

import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.PedidoValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
public class PagamentoService {
    
    private static final Logger logger = Logger.getLogger(PagamentoService.class.getName());
    
    private final PedidoRepository pedidoRepository;
    private final PedidoValidator pedidoValidator;
    private final PagamentoServiceClient pagamentoServiceClient;
    
    public PagamentoService(PedidoRepository pedidoRepository,
                           PedidoValidator pedidoValidator,
                           PagamentoServiceClient pagamentoServiceClient) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoValidator = pedidoValidator;
        this.pagamentoServiceClient = pagamentoServiceClient;
    }
    
    @Transactional
    public PagamentoResponseDTO criarPagamento(Long pedidoId, CartaoCreditoRequestDTO cartaoDTO, String remoteIp) {
        Pedido pedido = buscarPedidoValido(pedidoId);
        validatePedidoOwnership(pedido);
        pedidoValidator.validateStatusParaConfirmacao(pedido);
        
        PagamentoResponseDTO response = pagamentoServiceClient.criarPagamento(pedido, cartaoDTO, remoteIp);
        
        if (pedido.getMetodoPagamento() == MetodoPagamento.CASH) {
            pedido.setStatus(StatusPedido.CONFIRMED);
            pedidoRepository.save(pedido);
        } else if (response.getStatus() == StatusPagamento.AUTHORIZED) {
            pedido.setStatus(StatusPedido.CONFIRMED);
            pedidoRepository.save(pedido);
        }
        
        return response;
    }
    
    private Pedido buscarPedidoValido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        SecurityUtils.validatePedidoOwnership(pedido);
    }
    
    @Transactional
    public PagamentoResponseDTO buscarPagamentoPorPedido(Long pedidoId) {
        Pedido pedido = buscarPedidoValido(pedidoId);
        validatePedidoOwnership(pedido);
        
        PagamentoResponseDTO response = pagamentoServiceClient.buscarPagamentoPorPedido(pedidoId);
        
        if (response.getStatus() == StatusPagamento.PAID && pedido.getStatus() == StatusPedido.CREATED) {
            pedido.setStatus(StatusPedido.CONFIRMED);
            pedidoRepository.save(pedido);
        }
        
        return response;
    }
    
    @Transactional
    public PagamentoResponseDTO processarReembolso(Long pedidoId, String motivo) {
        Pedido pedido = buscarPedidoValido(pedidoId);
        
        PagamentoResponseDTO pagamentoAtual = pagamentoServiceClient.buscarPagamentoPorPedido(pedidoId);
        
        if (pagamentoAtual.getStatus() == StatusPagamento.REFUNDED) {
            throw new com.siseg.exception.PagamentoJaReembolsadoException("Pagamento já foi reembolsado");
        }
        
        if (pagamentoAtual.getStatus() != StatusPagamento.PAID && 
            pagamentoAtual.getStatus() != StatusPagamento.AUTHORIZED) {
            throw new IllegalStateException("Apenas pagamentos PAID ou AUTHORIZED podem ser reembolsados");
        }
        
        PagamentoResponseDTO response = pagamentoServiceClient.processarReembolso(pedidoId, motivo);
        
        pedido.setStatus(StatusPedido.CANCELED);
        pedidoRepository.save(pedido);
        
        logger.info("Reembolso processado para pedido " + pedidoId + " - Valor: R$ " + response.getValorReembolsado());
        
        return response;
    }
}
