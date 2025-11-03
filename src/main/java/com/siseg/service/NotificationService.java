package com.siseg.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * Serviço de notificações
 * Por enquanto apenas loga as notificações. Estrutura preparada para integração futura com email/SMS
 */
@Service
public class NotificationService {
    
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());
    
    /**
     * Envia notificação de email (simulado via log)
     */
    public void sendEmail(String to, String subject, String message) {
        logger.info(String.format("NOTIFICAÇÃO EMAIL SIMULADA:\nDestinatário: %s\nAssunto: %s\nMensagem: %s", 
                to, subject, message));
        // TODO: Implementar integração real com serviço de email (ex: SendGrid, AWS SES)
    }
    
    /**
     * Envia notificação SMS (simulado via log)
     */
    public void sendSMS(String phoneNumber, String message) {
        logger.info(String.format("NOTIFICAÇÃO SMS SIMULADA:\nDestinatário: %s\nMensagem: %s", 
                phoneNumber, message));
        // TODO: Implementar integração real com serviço de SMS (ex: Twilio)
    }
    
    /**
     * Notifica cliente sobre mudança de status do pedido
     */
    public void notifyOrderStatusChange(Long pedidoId, String clienteEmail, String clienteTelefone, String status) {
        String subject = String.format("Pedido #%d - Status atualizado", pedidoId);
        String message = String.format("Seu pedido #%d teve o status atualizado para: %s", pedidoId, status);
        
        if (clienteEmail != null) {
            sendEmail(clienteEmail, subject, message);
        }
        
        // SMS opcional (comentado por enquanto)
        // if (clienteTelefone != null) {
        //     sendSMS(clienteTelefone, message);
        // }
    }
    
    /**
     * Notifica entregador sobre novo pedido disponível
     */
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
    
    /**
     * Notifica restaurante sobre novo pedido
     */
    public void notifyRestaurantNewOrder(Long pedidoId, String restauranteEmail, BigDecimal valorTotal) {
        String subject = String.format("Novo pedido #%d recebido", pedidoId);
        String message = String.format("Você recebeu um novo pedido #%d no valor de R$ %.2f", 
                pedidoId, valorTotal);
        
        if (restauranteEmail != null) {
            sendEmail(restauranteEmail, subject, message);
        }
    }
}

