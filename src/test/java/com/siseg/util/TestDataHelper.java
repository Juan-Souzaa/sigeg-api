package com.siseg.util;

import com.siseg.model.Cliente;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.RestauranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestDataHelper {

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TestJwtUtil testJwtUtil;

    /**
     * Cria um restaurante para um usuário específico
     */
    public Restaurante createRestauranteForUser(String username) {
        User user = testJwtUtil.getOrCreateUser(username, ERole.ROLE_USER);
        
        Restaurante restaurante = new Restaurante();
        restaurante.setNome("Restaurante de " + username);
        restaurante.setEmail(username + "@restaurante.com");
        restaurante.setTelefone("(11) 99999-9999");
        restaurante.setEndereco("Rua Teste, 123");
        restaurante.setStatus(StatusRestaurante.APPROVED);
        restaurante.setUser(user);
        
        return restauranteRepository.save(restaurante);
    }

    /**
     * Cria um cliente para um usuário específico
     */
    public Cliente createClienteForUser(String username) {
        User user = testJwtUtil.getOrCreateUser(username, ERole.ROLE_CLIENTE);
        
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente " + username);
        cliente.setEmail(username + "@email.com");
        cliente.setTelefone("(11) 88888-8888");
        cliente.setEndereco("Rua Cliente, 456");
        cliente.setUser(user);
        
        return clienteRepository.save(cliente);
    }

    /**
     * Gera token para um usuário e cria restaurante para ele
     */
    public RestauranteData createRestauranteWithOwner(String username) {
        String token = testJwtUtil.generateTokenForUser(username, ERole.ROLE_USER);
        Restaurante restaurante = createRestauranteForUser(username);
        return new RestauranteData(restaurante, token);
    }

    /**
     * Gera token para um usuário e cria cliente para ele
     */
    public ClienteData createClienteWithOwner(String username) {
        String token = testJwtUtil.generateTokenForUser(username, ERole.ROLE_CLIENTE);
        Cliente cliente = createClienteForUser(username);
        return new ClienteData(cliente, token);
    }

    public static class RestauranteData {
        public final Restaurante restaurante;
        public final String token;

        public RestauranteData(Restaurante restaurante, String token) {
            this.restaurante = restaurante;
            this.token = token;
        }
    }

    public static class ClienteData {
        public final Cliente cliente;
        public final String token;

        public ClienteData(Cliente cliente, String token) {
            this.cliente = cliente;
            this.token = token;
        }
    }
}

