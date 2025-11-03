package com.siseg.util;

import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import com.siseg.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TestJwtUtil {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Gera um token JWT para um usuário admin
     */
    public String generateAdminToken() {
        User admin = getOrCreateAdminUser();
        return jwtService.generateToken(admin);
    }

    /**
     * Gera um token JWT para um usuário comum
     */
    public String generateUserToken() {
        User user = getOrCreateUser("testuser", ERole.ROLE_USER);
        return jwtService.generateToken(user);
    }

    /**
     * Gera um token JWT para um usuário específico
     */
    public String generateTokenForUser(String username, ERole role) {
        User user = getOrCreateUser(username, role);
        return jwtService.generateToken(user);
    }

    private User getOrCreateAdminUser() {
        return getOrCreateUser("admin", ERole.ROLE_ADMIN);
    }

    private User getOrCreateUser(String username, ERole roleName) {
        try {
            return userRepository.findByUsername(username)
                    .orElseGet(() -> createUser(username, roleName));
        } catch (Exception e) {
            // Se ainda não existe a tabela, cria o usuário
            return createUser(username, roleName);
        }
    }
    
    private User createUser(String username, ERole roleName) {
        // Criar role se não existir
        Role role = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(roleName);
                    return roleRepository.save(newRole);
                });

        // Criar usuário
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password123"));
        
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        
        return userRepository.save(user);
    }
}

