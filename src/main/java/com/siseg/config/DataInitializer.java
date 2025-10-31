package com.siseg.config;

import com.siseg.model.Cliente;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Criar roles se não existirem
        if (roleRepository.count() == 0) {
            createRoles();
        }

        // Criar usuário admin se não existir
        if (userRepository.findByUsername("admin").isEmpty()) {
            createAdminUser();
        }

        // Criar clientes de exemplo se não existirem
        if (clienteRepository.count() == 0) {
            createSampleClients();
        }
    }

    private void createRoles() {
        Role adminRole = new Role();
        adminRole.setRoleName(ERole.ROLE_ADMIN);
        roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setRoleName(ERole.ROLE_USER);
        roleRepository.save(userRole);

        Role restauranteRole = new Role();
        restauranteRole.setRoleName(ERole.ROLE_RESTAURANTE);
        roleRepository.save(restauranteRole);

        Role entregadorRole = new Role();
        entregadorRole.setRoleName(ERole.ROLE_ENTREGADOR);
        roleRepository.save(entregadorRole);

        Role clienteRole = new Role();
        clienteRole.setRoleName(ERole.ROLE_CLIENTE);
        roleRepository.save(clienteRole);

        System.out.println("✅ Roles criados com sucesso!");
    }

    private void createAdminUser() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));

        Set<Role> roles = new HashSet<>();
        Role adminRole = roleRepository.findByRoleName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Role ADMIN não encontrado"));
        roles.add(adminRole);

        admin.setRoles(roles);
        userRepository.save(admin);

        System.out.println("✅ Usuário admin criado com sucesso!");
        System.out.println("   Username: admin");
        System.out.println("   Password: admin123");
    }

    private void createSampleClients() {
        // Cliente 1
        User user1 = new User();
        user1.setUsername("joao.silva@email.com");
        user1.setPassword(passwordEncoder.encode("123456"));
        
        Set<Role> roles1 = new HashSet<>();
        Role clienteRole = roleRepository.findByRoleName(ERole.ROLE_CLIENTE)
                .orElseThrow(() -> new RuntimeException("Role CLIENTE não encontrado"));
        roles1.add(clienteRole);
        user1.setRoles(roles1);
        
        User savedUser1 = userRepository.save(user1);
        
        Cliente cliente1 = new Cliente();
        cliente1.setUser(savedUser1);
        cliente1.setNome("João Silva");
        cliente1.setEmail("joao.silva@email.com");
        cliente1.setTelefone("11987654321");
        cliente1.setEndereco("Rua das Flores, 123 - São Paulo/SP");
        clienteRepository.save(cliente1);

        // Cliente 2
        User user2 = new User();
        user2.setUsername("maria.santos@email.com");
        user2.setPassword(passwordEncoder.encode("123456"));
        
        Set<Role> roles2 = new HashSet<>();
        roles2.add(clienteRole);
        user2.setRoles(roles2);
        
        User savedUser2 = userRepository.save(user2);
        
        Cliente cliente2 = new Cliente();
        cliente2.setUser(savedUser2);
        cliente2.setNome("Maria Santos");
        cliente2.setEmail("maria.santos@email.com");
        cliente2.setTelefone("11912345678");
        cliente2.setEndereco("Av. Paulista, 456 - São Paulo/SP");
        clienteRepository.save(cliente2);

        // Cliente 3
        User user3 = new User();
        user3.setUsername("pedro.oliveira@email.com");
        user3.setPassword(passwordEncoder.encode("123456"));
        
        Set<Role> roles3 = new HashSet<>();
        roles3.add(clienteRole);
        user3.setRoles(roles3);
        
        User savedUser3 = userRepository.save(user3);
        
        Cliente cliente3 = new Cliente();
        cliente3.setUser(savedUser3);
        cliente3.setNome("Pedro Oliveira");
        cliente3.setEmail("pedro.oliveira@email.com");
        cliente3.setTelefone("11955554444");
        cliente3.setEndereco("Rua Augusta, 789 - São Paulo/SP");
        clienteRepository.save(cliente3);

        System.out.println("✅ Clientes de exemplo criados com sucesso!");
        System.out.println("   Cliente 1: João Silva (ID: 1) - Email: joao.silva@email.com - Senha: 123456");
        System.out.println("   Cliente 2: Maria Santos (ID: 2) - Email: maria.santos@email.com - Senha: 123456");
        System.out.println("   Cliente 3: Pedro Oliveira (ID: 3) - Email: pedro.oliveira@email.com - Senha: 123456");
    }
}