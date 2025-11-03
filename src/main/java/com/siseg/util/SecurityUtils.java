package com.siseg.util;

import com.siseg.model.User;
import com.siseg.model.UserAuthenticated;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Utilitário para obter informações do usuário autenticado
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
}

