package com.siseg.config;

import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

/**
 * Inicializador de dados para testes
 * Cria roles e usuários básicos necessários para os testes
 */
@TestComponent
public class TestDataInitializer {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void initialize() {
        createRolesIfNotExist();
        createAdminUserIfNotExist();
        createTestUserIfNotExist();
    }

    private void createRolesIfNotExist() {
        for (ERole roleName : ERole.values()) {
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                Role role = new Role();
                role.setRoleName(roleName);
                roleRepository.save(role);
            }
        }
    }

    private void createAdminUserIfNotExist() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));

            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByRoleName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Role ADMIN não encontrado"));
            roles.add(adminRole);

            admin.setRoles(roles);
            userRepository.save(admin);
        }
    }

    private void createTestUserIfNotExist() {
        if (userRepository.findByUsername("testuser").isEmpty()) {
            User user = new User();
            user.setUsername("testuser");
            user.setPassword(passwordEncoder.encode("password123"));

            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByRoleName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Role USER não encontrado"));
            roles.add(userRole);

            user.setRoles(roles);
            userRepository.save(user);
        }
    }
}

