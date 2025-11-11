package com.siseg.service;

import com.siseg.dto.pagamento.AsaasWebhookDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Pagamento;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.PagamentoRepository;
import com.siseg.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
public class AsaasWebhookService {
    
    private static final Logger logger = Logger.getLogger(AsaasWebhookService.class.getName());
    
    @Value("${asaas.webhookSecret}")
    private String webhookSecret;
    
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    
    public AsaasWebhookService(PagamentoRepository pagamentoRepository, 
                               PedidoRepository pedidoRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
    }
    
    public boolean validarAssinatura(String signature, String payload) {
        return signature != null && signature.equals(webhookSecret);
    }
    
    @Transactional
    public void processarWebhook(AsaasWebhookDTO webhook) {
        if (!isEventoPagamentoValido(webhook)) {
            return;
        }
        
        String asaasPaymentId = webhook.getPayment().getId();
        Pagamento pagamento = buscarPagamentoPorAsaasId(asaasPaymentId);
        
        String evento = webhook.getEvent();
        processarEvento(evento, pagamento, asaasPaymentId);
        
        pagamentoRepository.save(pagamento);
        if (pagamento.getPedido() != null) {
            pedidoRepository.save(pagamento.getPedido());
        }
    }
    
    private boolean isEventoPagamentoValido(AsaasWebhookDTO webhook) {
        if (webhook == null || webhook.getEvent() == null || webhook.getPayment() == null) {
            return false;
        }
        String evento = webhook.getEvent();
        return "PAYMENT_RECEIVED".equals(evento) || 
               "PAYMENT_CONFIRMED".equals(evento) || 
               "PAYMENT_REFUSED".equals(evento);
    }
    
    private Pagamento buscarPagamentoPorAsaasId(String asaasPaymentId) {
        return pagamentoRepository.findByAsaasPaymentId(asaasPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + asaasPaymentId));
    }
    
    private void processarEvento(String evento, Pagamento pagamento, String asaasPaymentId) {
        if ("PAYMENT_RECEIVED".equals(evento) || "PAYMENT_CONFIRMED".equals(evento)) {
            processarPagamentoConfirmado(pagamento, asaasPaymentId);
        } else if ("PAYMENT_REFUSED".equals(evento)) {
            processarPagamentoRecusado(pagamento, asaasPaymentId);
        }
    }
    
    private void processarPagamentoConfirmado(Pagamento pagamento, String asaasPaymentId) {
        pagamento.setStatus(StatusPagamento.PAID);
        pagamento.setAtualizadoEm(java.time.Instant.now());
        
        Pedido pedido = pagamento.getPedido();
        if (pedido != null) {
            pedido.setStatus(StatusPedido.CONFIRMED);
            logger.info("Pagamento confirmado via webhook: " + asaasPaymentId + " - Pedido: " + pedido.getId());
        }
    }
    
    private void processarPagamentoRecusado(Pagamento pagamento, String asaasPaymentId) {
        pagamento.setStatus(StatusPagamento.REFUSED);
        Pedido pedido = pagamento.getPedido();
        if (pedido != null) {
            logger.warning("Pagamento recusado via webhook: " + asaasPaymentId + " - Pedido: " + pedido.getId());
        }
    }
}

