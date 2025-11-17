package com.siseg.validator;

import com.siseg.exception.PagamentoJaReembolsadoException;
import com.siseg.model.Pagamento;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import org.springframework.stereotype.Component;

@Component
public class PagamentoValidator {
    
    public void validateStatusPedido(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.CREATED) {
            throw new RuntimeException("Pedido já foi processado");
        }
    }
    
    public void validateReembolsoPossivel(Pagamento pagamento) {
        if (pagamento == null) {
            throw new IllegalArgumentException("Pagamento não pode ser nulo");
        }
        
        if (pagamento.getStatus() == StatusPagamento.REFUNDED) {
            throw new PagamentoJaReembolsadoException("Pagamento já foi reembolsado");
        }
        
        if (pagamento.getStatus() != StatusPagamento.PAID && 
            pagamento.getStatus() != StatusPagamento.AUTHORIZED) {
            throw new IllegalStateException("Apenas pagamentos PAID ou AUTHORIZED podem ser reembolsados");
        }
        
        if (pagamento.getAsaasPaymentId() == null && pagamento.getMetodo() != MetodoPagamento.CASH) {
            throw new IllegalStateException("Pagamento não possui ID do gateway para reembolso");
        }
    }
}

