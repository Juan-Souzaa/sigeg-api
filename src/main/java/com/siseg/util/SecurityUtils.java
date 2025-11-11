package com.siseg.util;

import com.siseg.exception.AccessDeniedException;
import com.siseg.model.*;
import com.siseg.model.User;
import com.siseg.model.UserAuthenticated;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Utilitário para obter informações do usuário autenticado e validações de acesso
 */
public class SecurityUtils {
    
    /**
     * Obtém o usuário autenticado atual
     * @return User autenticado
     * @throws UsernameNotFoundException se nenhum usuário estiver autenticado
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("Usuário não autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserAuthenticated) {
            return ((UserAuthenticated) principal).getUser();
        }
        
        throw new UsernameNotFoundException("Usuário não encontrado no contexto de segurança");
    }
    
    /**
     * Obtém o ID do usuário autenticado atual
     * @return ID do usuário
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    /**
     * Verifica se o usuário atual tem uma role específica
     * @param roleName Nome da role (ex: "ADMIN", "CLIENTE")
     * @return true se o usuário tem a role
     */
    public static boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + roleName));
    }
    
    /**
     * Verifica se o usuário atual é admin
     * @return true se o usuário é admin
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    /**
     * Valida se o usuário autenticado é admin.
     * @throws AccessDeniedException se o usuário não for admin
     */
    public static void validateAdminAccess() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Apenas administradores podem realizar esta operacao");
        }
    }
    
    /**
     * Valida se o usuário autenticado tem permissão para acessar o pedido.
     * Permite acesso se o usuário for admin, dono do cliente, dono do restaurante ou entregador associado.
     * @param pedido Pedido a ser validado
     * @throws AccessDeniedException se o usuário não tiver permissão
     */
    public static void validatePedidoOwnership(Pedido pedido) {
        if (pedido == null) {
            throw new AccessDeniedException("Pedido não encontrado");
        }
        
        if (isAdmin()) {
            return;
        }
        
        User currentUser = getCurrentUser();
        
        if (pedido.getCliente() != null && pedido.getCliente().getUser() != null && 
            pedido.getCliente().getUser().getId().equals(currentUser.getId())) {
            return;
        }
        
        if (pedido.getRestaurante() != null && pedido.getRestaurante().getUser() != null &&
            pedido.getRestaurante().getUser().getId().equals(currentUser.getId())) {
            return;
        }
        
        if (pedido.getEntregador() != null && pedido.getEntregador().getUser() != null &&
            pedido.getEntregador().getUser().getId().equals(currentUser.getId())) {
            return;
        }
        
        throw new AccessDeniedException("Você não tem permissão para acessar este pedido");
    }
    
    /**
     * Valida se o usuário autenticado tem permissão para acessar o restaurante.
     * Permite acesso se o usuário for admin ou dono do restaurante.
     * @param restaurante Restaurante a ser validado
     * @throws AccessDeniedException se o usuário não tiver permissão
     */
    public static void validateRestauranteOwnership(Restaurante restaurante) {
        if (restaurante == null) {
            throw new AccessDeniedException("Restaurante não encontrado");
        }
        
        if (isAdmin()) {
            return;
        }
        
        User currentUser = getCurrentUser();
        
        if (restaurante.getUser() == null || !restaurante.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Você não tem permissão para acessar este restaurante");
        }
    }
    
    /**
     * Valida se o usuário autenticado tem permissão para acessar o entregador.
     * Permite acesso se o usuário for admin ou o próprio entregador.
     * @param entregador Entregador a ser validado
     * @throws AccessDeniedException se o usuário não tiver permissão
     */
    public static void validateEntregadorOwnership(Entregador entregador) {
        if (entregador == null) {
            throw new AccessDeniedException("Entregador não encontrado");
        }
        
        if (isAdmin()) {
            return;
        }
        
        User currentUser = getCurrentUser();
        
        if (entregador.getUser() == null || !entregador.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Você não tem permissão para acessar este entregador");
        }
    }
}

