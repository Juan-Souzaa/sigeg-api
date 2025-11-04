package com.siseg.validator;

import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.PedidoAlreadyProcessedException;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.EntregadorRepository;
import org.springframework.stereotype.Component;

@Component
public class PedidoValidator {
    
    private final EntregadorRepository entregadorRepository;
    
    public PedidoValidator(EntregadorRepository entregadorRepository) {
        this.entregadorRepository = entregadorRepository;
    }
    
    public Entregador validateEntregadorAprovado(User currentUser) {
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não é entregador"));
        
        if (entregador.getStatus() != StatusEntregador.APPROVED) {
            throw new AccessDeniedException("Entregador não está aprovado");
        }
        return entregador;
    }
    
    public void validatePedidoAceitavel(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.PREPARING) {
            throw new PedidoAlreadyProcessedException("Pedido deve estar PREPARING para ser aceito");
        }
        if (pedido.getEntregador() != null) {
            throw new PedidoAlreadyProcessedException("Pedido já foi aceito por outro entregador");
        }
    }
    
    public void validateStatusPreparo(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.CONFIRMED) {
            throw new PedidoAlreadyProcessedException("Pedido deve estar CONFIRMED para ser marcado como PREPARING");
        }
    }
}

