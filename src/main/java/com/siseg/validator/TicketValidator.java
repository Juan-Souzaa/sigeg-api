package com.siseg.validator;

import com.siseg.exception.AccessDeniedException;
import com.siseg.model.Ticket;
import com.siseg.model.TicketComentario;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.util.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TicketValidator {
    
    public void validatePermissaoCriarTicket(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toSet());
        
        boolean temRoleValida = roles.contains(ERole.ROLE_CLIENTE.name()) ||
                               roles.contains(ERole.ROLE_RESTAURANTE.name()) ||
                               roles.contains(ERole.ROLE_ENTREGADOR.name()) ||
                               roles.contains(ERole.ROLE_ADMIN.name());
        
        if (!temRoleValida) {
            throw new AccessDeniedException("Apenas clientes, restaurantes, entregadores ou administradores podem criar tickets");
        }
    }
    
    public void validatePermissaoResponderTicket(Ticket ticket) {
        if (!SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Apenas administradores podem responder tickets");
        }
    }
    
    public void validatePermissaoVisualizarComentarioInterno(TicketComentario comentario, User user) {
        if (comentario.getInterno() && !SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("Apenas administradores podem visualizar comentarios internos");
        }
    }
    
    public void validateTicketAberto(Ticket ticket) {
        if (ticket.getStatus() == StatusTicket.RESOLVIDO || ticket.getStatus() == StatusTicket.FECHADO) {
            throw new IllegalStateException("Ticket ja esta resolvido ou fechado");
        }
    }
    
    public void validatePermissaoComentarTicket(Ticket ticket, User user) {
        boolean eAdmin = SecurityUtils.isAdmin();
        boolean eCriador = ticket.getCriadoPor().getId().equals(user.getId());
        
        if (!eAdmin && !eCriador) {
            throw new AccessDeniedException("Apenas o criador do ticket ou administradores podem comentar");
        }
    }
    
    public void validatePermissaoVisualizarTicket(Ticket ticket, User user) {
        boolean eAdmin = SecurityUtils.isAdmin();
        boolean eCriador = ticket.getCriadoPor().getId().equals(user.getId());
        boolean eAtribuido = ticket.getAtribuidoA() != null && 
                            ticket.getAtribuidoA().getId().equals(user.getId());
        
        if (!eAdmin && !eCriador && !eAtribuido) {
            throw new AccessDeniedException("Voce nao tem permissao para visualizar este ticket");
        }
    }
}

