package com.siseg.validator;

import org.springframework.stereotype.Component;

import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.PedidoAlreadyProcessedException;
import com.siseg.exception.PratoNotAvailableException;
import com.siseg.model.Cliente;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Prato;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.EntregadorRepository;
import com.siseg.util.SecurityUtils;

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
    
    public void validateEntregadorDoPedido(Pedido pedido, String mensagemErro) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        if (pedido.getEntregador() == null) {
            throw new AccessDeniedException(mensagemErro);
        }
        
        SecurityUtils.validateEntregadorOwnership(pedido.getEntregador());
    }
    
    public void validateStatusParaConfirmacao(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.CREATED) {
            throw new PedidoAlreadyProcessedException("Pedido já foi processado");
        }
    }
    
    public void validateStatusParaSaiuEntrega(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.PREPARING) {
            throw new PedidoAlreadyProcessedException("Pedido deve estar PREPARING para ser marcado como OUT_FOR_DELIVERY");
        }
    }
    
    public void validateStatusParaEntrega(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.OUT_FOR_DELIVERY) {
            throw new PedidoAlreadyProcessedException("Pedido deve estar OUT_FOR_DELIVERY para ser marcado como DELIVERED");
        }
    }
    
    public void validatePermissaoCliente(Cliente cliente, User currentUser) {
        if (!SecurityUtils.isAdmin() && (cliente.getUser() == null || !cliente.getUser().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Você não tem permissão para criar pedidos para este cliente");
        }
    }
    
    public Prato validatePratoDisponivel(Prato prato) {
        if (!prato.getDisponivel()) {
            throw new PratoNotAvailableException("Prato não disponível: " + prato.getNome());
        }
        return prato;
    }
    
    public void validateStatusEntrega(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.OUT_FOR_DELIVERY) {
            throw new IllegalStateException("Pedido não está em entrega");
        }
    }
    
    public void validateEntregadorAssociado(Pedido pedido) {
        if (pedido.getEntregador() == null) {
            throw new IllegalStateException("Pedido sem entregador");
        }
    }
    
    public void validateCoordenadasDestino(Pedido pedido) {
        if (pedido.getLatitudeEntrega() == null || pedido.getLongitudeEntrega() == null) {
            throw new IllegalStateException("Pedido sem coordenadas de destino");
        }
    }
}

