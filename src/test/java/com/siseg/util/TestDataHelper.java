package com.siseg.util;

import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.EnderecoService;
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

    @Autowired
    private EnderecoService enderecoService;

    /**
     * Cria um restaurante para um usuário específico
     */
    public Restaurante createRestauranteForUser(String username) {
        User user = testJwtUtil.getOrCreateUser(username, ERole.ROLE_USER);
        
        Restaurante restaurante = new Restaurante();
        restaurante.setNome("Restaurante de " + username);
        restaurante.setEmail(username + "@restaurante.com");
        restaurante.setTelefone("(11) 99999-9999");
        restaurante.setStatus(StatusRestaurante.APPROVED);
        restaurante.setUser(user);
        
        Restaurante saved = restauranteRepository.save(restaurante);
        
        // Criar endereço para o restaurante
        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro("Rua Teste");
        enderecoDTO.setNumero("123");
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(true);
        
        enderecoService.criarEndereco(enderecoDTO, saved);
        
        return saved;
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
        cliente.setUser(user);
        
        Cliente saved = clienteRepository.save(cliente);
        
        // Criar endereço para o cliente
        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro("Rua Cliente");
        enderecoDTO.setNumero("456");
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(true);
        
        enderecoService.criarEndereco(enderecoDTO, saved);
        
        return saved;
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

