package com.siseg.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.logging.Logger;

@Service
public class NotificationService {
    
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());
    
    public void sendEmail(String to, String subject, String message) {
        logger.info(String.format("NOTIFICAÇÃO EMAIL SIMULADA:\nDestinatário: %s\nAssunto: %s\nMensagem: %s", 
                to, subject, message));
    }
    
    public void sendSMS(String phoneNumber, String message) {
        logger.info(String.format("NOTIFICAÇÃO SMS SIMULADA:\nDestinatário: %s\nMensagem: %s", 
                phoneNumber, message));
    }
    
    public void notifyOrderStatusChange(Long pedidoId, String clienteEmail, String clienteTelefone, String status) {
        String subject = String.format("Pedido #%d - Status atualizado", pedidoId);
        String message = String.format("Seu pedido #%d teve o status atualizado para: %s", pedidoId, status);
        
        if (clienteEmail != null) {
            sendEmail(clienteEmail, subject, message);
        }
    }
    
    public void notifyNewOrderAvailable(Long pedidoId, String entregadorEmail, String entregadorTelefone, 
                                       String enderecoEntrega, BigDecimal valorTotal) {
        String subject = "Novo pedido disponível para entrega";
        String message = String.format("Novo pedido #%d disponível!\nEndereço: %s\nValor: R$ %.2f", 
                pedidoId, enderecoEntrega, valorTotal);
        
        if (entregadorEmail != null) {
            sendEmail(entregadorEmail, subject, message);
        }
        
        if (entregadorTelefone != null) {
            sendSMS(entregadorTelefone, message);
        }
    }
    
    public void notifyRestaurantNewOrder(Long pedidoId, String restauranteEmail, BigDecimal valorTotal) {
        String subject = String.format("Novo pedido #%d recebido", pedidoId);
        String message = String.format("Você recebeu um novo pedido #%d no valor de R$ %.2f", 
                pedidoId, valorTotal);
        
        if (restauranteEmail != null) {
            sendEmail(restauranteEmail, subject, message);
        }
    }
}

