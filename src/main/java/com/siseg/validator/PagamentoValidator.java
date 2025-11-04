package com.siseg.validator;

import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPedido;
import org.springframework.stereotype.Component;

@Component
public class PagamentoValidator {
    
    public void validateStatusPedido(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.CREATED) {
            throw new RuntimeException("Pedido já foi processado");
        }
    }
}

