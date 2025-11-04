package com.siseg.validator;

import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.AvaliacaoAlreadyExistsException;
import com.siseg.model.Avaliacao;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.util.SecurityUtils;
import org.springframework.stereotype.Component;

@Component
public class AvaliacaoValidator {
    
    private final AvaliacaoRepository avaliacaoRepository;
    
    public AvaliacaoValidator(AvaliacaoRepository avaliacaoRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
    }
    
    public void validatePermissaoAvaliacao(Pedido pedido) {
        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.validatePedidoOwnership(pedido);
        }
    }
    
    public void validatePedidoEntregue(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.DELIVERED) {
            throw new IllegalStateException("Apenas pedidos entregues (DELIVERED) podem ser avaliados");
        }
    }
    
    public void validateAvaliacaoNaoExistente(Long clienteId, Long pedidoId) {
        if (avaliacaoRepository.existsByClienteIdAndPedidoId(clienteId, pedidoId)) {
            throw new AvaliacaoAlreadyExistsException("Já existe uma avaliação para este pedido. Use o endpoint de edição para atualizar.");
        }
    }
    
    public void validateOwnership(Avaliacao avaliacao, User currentUser) {
        if (avaliacao.getCliente() == null || avaliacao.getCliente().getUser() == null ||
            !avaliacao.getCliente().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Você só pode editar suas próprias avaliações");
        }
    }
}

