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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.logging.Logger;

@Service
public class PagamentoService {
    
    private static final Logger logger = Logger.getLogger(PagamentoService.class.getName());
    
    private final PedidoRepository pedidoRepository;
    private final PedidoValidator pedidoValidator;
    private final PagamentoServiceClient pagamentoServiceClient;
    private final Validator validator;
    
    public PagamentoService(PedidoRepository pedidoRepository,
                           PedidoValidator pedidoValidator,
                           PagamentoServiceClient pagamentoServiceClient,
                           Validator validator) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoValidator = pedidoValidator;
        this.pagamentoServiceClient = pagamentoServiceClient;
        this.validator = validator;
    }
    
    @Transactional
    public PagamentoResponseDTO criarPagamento(Long pedidoId, CartaoCreditoRequestDTO cartaoDTO, String remoteIp) {
        Pedido pedido = buscarPedidoValido(pedidoId);
        validatePedidoOwnership(pedido);
        pedidoValidator.validateStatusParaConfirmacao(pedido);
        
        CartaoCreditoRequestDTO cartao = normalizarCartao(cartaoDTO);
        validarCartaoSeCredito(pedido, cartao);
        
        PagamentoResponseDTO response = pagamentoServiceClient.criarPagamento(pedido, cartao, remoteIp);
        
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
    
    private CartaoCreditoRequestDTO normalizarCartao(CartaoCreditoRequestDTO cartaoDTO) {
        if (cartaoDTO == null) {
            return null;
        }
        if (cartaoDTO.getNumero() == null || cartaoDTO.getNumero().isBlank()) {
            return null;
        }
        return cartaoDTO;
    }
    
    private void validarCartaoSeCredito(Pedido pedido, CartaoCreditoRequestDTO cartao) {
        if (pedido.getMetodoPagamento() != MetodoPagamento.CREDIT_CARD) {
            return;
        }
        if (cartao == null) {
            throw new IllegalArgumentException("Dados do cartão são obrigatórios para pagamento com cartão de crédito");
        }
        Set<ConstraintViolation<CartaoCreditoRequestDTO>> violations = validator.validate(cartao);
        if (!violations.isEmpty()) {
            ConstraintViolation<CartaoCreditoRequestDTO> first = violations.iterator().next();
            throw new IllegalArgumentException(first.getMessage());
        }
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
