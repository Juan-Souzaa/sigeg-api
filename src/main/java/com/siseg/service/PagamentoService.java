package com.siseg.service;

import com.siseg.dto.pagamento.AsaasPaymentResponseDTO;
import com.siseg.dto.pagamento.AsaasQrCodeResponseDTO;
import com.siseg.dto.pagamento.AsaasRefundResponseDTO;
import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.exception.PaymentGatewayException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Pagamento;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.PagamentoRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.PagamentoValidator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
public class PagamentoService {
    
    private static final Logger logger = Logger.getLogger(PagamentoService.class.getName());
    
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final ModelMapper modelMapper;
    private final PagamentoValidator pagamentoValidator;
    private final AsaasService asaasService;
    
    public PagamentoService(PagamentoRepository pagamentoRepository, 
                           PedidoRepository pedidoRepository,
                           ModelMapper modelMapper,
                           PagamentoValidator pagamentoValidator,
                           AsaasService asaasService) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.modelMapper = modelMapper;
        this.pagamentoValidator = pagamentoValidator;
        this.asaasService = asaasService;
    }
    
    @Transactional
    public PagamentoResponseDTO criarPagamento(Long pedidoId, CartaoCreditoRequestDTO cartaoDTO, String remoteIp) {
        Pedido pedido = buscarPedidoValido(pedidoId);
        validatePedidoOwnership(pedido);
        pagamentoValidator.validateStatusPedido(pedido);
        
        Pagamento pagamento = criarPagamentoBasico(pedido);
        
        if (pedido.getMetodoPagamento() == MetodoPagamento.PIX) {
            processarPagamentoPix(pagamento);
        } else if (pedido.getMetodoPagamento() == MetodoPagamento.CREDIT_CARD) {
            if (cartaoDTO == null) {
                throw new IllegalArgumentException("Dados do cartão são obrigatórios para pagamento com cartão de crédito");
            }
            processarPagamentoCartao(pagamento, cartaoDTO, remoteIp);
        } else {
            processarPagamentoDinheiro(pagamento, pedido);
        }
        
        Pagamento saved = pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);
        
        PagamentoResponseDTO response = modelMapper.map(saved, PagamentoResponseDTO.class);
        response.setPedidoId(saved.getPedido().getId());
        return response;
    }
    
    private Pedido buscarPedidoValido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
    }
    
    private Pagamento criarPagamentoBasico(Pedido pedido) {
        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setMetodo(pedido.getMetodoPagamento());
        pagamento.setValor(pedido.getTotal());
        pagamento.setTroco(pedido.getTroco());
        pagamento.setStatus(StatusPagamento.PENDING);
        return pagamento;
    }
    
    private void processarPagamentoDinheiro(Pagamento pagamento, Pedido pedido) {
        pagamento.setStatus(StatusPagamento.PENDING);
        pedido.setStatus(StatusPedido.CONFIRMED);
    }
    
    private void processarPagamentoPix(Pagamento pagamento) {
        try {
            String asaasCustomerId = asaasService.buscarOuCriarCliente(pagamento.getPedido().getCliente());
            AsaasPaymentResponseDTO response = asaasService.criarPagamentoPix(pagamento, asaasCustomerId);
            
            validarRespostaAsaas(response);
            atualizarPagamentoComRespostaAsaas(pagamento, asaasCustomerId, response);
            atualizarPagamentoComQrCode(pagamento, response.getId());
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão com API Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao criar pagamento PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar pagamento PIX: " + e.getMessage());
        }
    }
    
    private void processarPagamentoCartao(Pagamento pagamento, CartaoCreditoRequestDTO cartaoDTO, String remoteIp) {
        try {
            String asaasCustomerId = asaasService.buscarOuCriarCliente(pagamento.getPedido().getCliente());
            AsaasPaymentResponseDTO response = asaasService.criarPagamentoCartao(pagamento, asaasCustomerId, cartaoDTO, remoteIp);
            
            validarRespostaAsaas(response);
            atualizarPagamentoComRespostaAsaas(pagamento, asaasCustomerId, response);
            
            if (response.getStatus() != null && "CONFIRMED".equals(response.getStatus())) {
                pagamento.setStatus(StatusPagamento.AUTHORIZED);
                pagamento.getPedido().setStatus(StatusPedido.CONFIRMED);
            } else {
                pagamento.setStatus(StatusPagamento.PENDING);
            }
            
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            logger.severe("Erro do Asaas (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            throw new PaymentGatewayException("Erro ao processar pagamento: " + e.getResponseBodyAsString(), e);
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão com API Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao criar pagamento com cartão: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar pagamento com cartão de crédito: " + e.getMessage());
        }
    }
    
    private void validarRespostaAsaas(AsaasPaymentResponseDTO response) {
        if (response == null) {
            throw new PaymentGatewayException("Resposta nula da API Asaas");
        }
    }
    
    private void atualizarPagamentoComRespostaAsaas(Pagamento pagamento, String asaasCustomerId, AsaasPaymentResponseDTO response) {
        pagamento.setAsaasPaymentId(response.getId());
        pagamento.setAsaasCustomerId(asaasCustomerId);
        pagamento.setStatus(StatusPagamento.AUTHORIZED);
    }
    
    private void atualizarPagamentoComQrCode(Pagamento pagamento, String asaasPaymentId) {
        try {
            AsaasQrCodeResponseDTO qrCodeResponse = asaasService.buscarQrCodePix(asaasPaymentId);
            if (qrCodeResponse != null) {
                pagamento.setQrCode(qrCodeResponse.getPayload());
                pagamento.setQrCodeImageUrl(qrCodeResponse.getEncodedImage());
            }
        } catch (Exception e) {
            logger.severe("Erro ao obter QR Code PIX: " + e.getMessage());
        }
    }
    
    
    public PagamentoResponseDTO buscarPagamentoPorPedido(Long pedidoId) {
        Pagamento pagamento = buscarPagamentoPorPedidoId(pedidoId);
        validatePedidoOwnership(pagamento.getPedido());
        PagamentoResponseDTO response = modelMapper.map(pagamento, PagamentoResponseDTO.class);
        response.setPedidoId(pagamento.getPedido().getId());
        return response;
    }
    
    private Pagamento buscarPagamentoPorPedidoId(Long pedidoId) {
        return pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado para o pedido: " + pedidoId));
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        SecurityUtils.validatePedidoOwnership(pedido);
    }
    
    @Transactional
    public PagamentoResponseDTO processarReembolso(Long pedidoId, String motivo) {
        Pedido pedido = buscarPedidoValido(pedidoId);
        Pagamento pagamento = buscarPagamentoPorPedidoId(pedidoId);
        
        pagamentoValidator.validateReembolsoPossivel(pagamento);
        
        if (pagamento.getMetodo() == MetodoPagamento.CASH) {
            processarReembolsoDinheiro(pagamento, motivo);
        } else {
            processarReembolsoEletronico(pagamento, motivo);
        }
        
        Pagamento saved = pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);
        
        logger.info("Reembolso processado para pedido " + pedidoId + " - Valor: R$ " + pagamento.getValorReembolsado());
        
        PagamentoResponseDTO response = modelMapper.map(saved, PagamentoResponseDTO.class);
        response.setPedidoId(saved.getPedido().getId());
        return response;
    }
    
    private void processarReembolsoDinheiro(Pagamento pagamento, String motivo) {
        pagamento.setStatus(StatusPagamento.REFUNDED);
        pagamento.setValorReembolsado(pagamento.getValor());
        pagamento.setDataReembolso(java.time.Instant.now());
        pagamento.setAtualizadoEm(java.time.Instant.now());
        
        if (pagamento.getPedido() != null) {
            pagamento.getPedido().setStatus(StatusPedido.CANCELED);
        }
        
        logger.info("Reembolso de dinheiro processado - Motivo: " + motivo);
    }
    
    private void processarReembolsoEletronico(Pagamento pagamento, String motivo) {
        try {
            String descricao = motivo != null ? motivo : "Reembolso de pedido cancelado";
            AsaasRefundResponseDTO refundResponse = asaasService.estornarPagamento(
                pagamento.getAsaasPaymentId(), 
                descricao
            );
            
            atualizarPagamentoComReembolso(pagamento, refundResponse);
            
            if (pagamento.getPedido() != null) {
                pagamento.getPedido().setStatus(StatusPedido.CANCELED);
            }
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão ao processar reembolso: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento ao processar reembolso.", e);
        } catch (Exception e) {
            logger.severe("Erro ao processar reembolso: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar reembolso: " + e.getMessage());
        }
    }
    
    private void atualizarPagamentoComReembolso(Pagamento pagamento, AsaasRefundResponseDTO refundResponse) {
        pagamento.setStatus(StatusPagamento.REFUNDED);
        pagamento.setValorReembolsado(new java.math.BigDecimal(refundResponse.getValue()));
        pagamento.setDataReembolso(java.time.Instant.now());
        pagamento.setAsaasRefundId(refundResponse.getId());
        pagamento.setAtualizadoEm(java.time.Instant.now());
    }
}
