package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.EnderecoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.EnderecoService;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnderecoControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private EnderecoService enderecoService;

    private String clienteToken;
    private String restauranteToken;
    private Long clienteId;
    private Long restauranteId;
    private Long enderecoClienteId;

    private EnderecoRequestDTO criarEnderecoDTO(String logradouro, String numero) {
        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro(logradouro);
        enderecoDTO.setNumero(numero);
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(false);
        return enderecoDTO;
    }

    @BeforeEach
    void setUp() {
        try {
            clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
            restauranteToken = testJwtUtil.generateTokenForUser("testrestaurante", ERole.ROLE_RESTAURANTE);
        } catch (Exception e) {
            try {
                Thread.sleep(100);
                clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
                restauranteToken = testJwtUtil.generateTokenForUser("testrestaurante", ERole.ROLE_RESTAURANTE);
            } catch (Exception ex) {
                throw new RuntimeException("Erro ao gerar tokens JWT para testes", ex);
            }
        }

        User clienteUser = testJwtUtil.getOrCreateUser("testcliente", ERole.ROLE_CLIENTE);
        Cliente cliente = clienteRepository.findByUserId(clienteUser.getId())
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setUser(clienteUser);
                    c.setNome("Cliente Teste");
                    c.setEmail("cliente@teste.com");
                    c.setTelefone("(11) 99999-9999");
                    return clienteRepository.save(c);
                });
        clienteId = cliente.getId();
        
        var enderecos = enderecoRepository.findByClienteId(clienteId);
        if (enderecos.isEmpty()) {
            Endereco endereco = new Endereco();
            endereco.setCliente(cliente);
            endereco.setLogradouro("Rua Teste");
            endereco.setNumero("123");
            endereco.setBairro("Centro");
            endereco.setCidade("São Paulo");
            endereco.setEstado("SP");
            endereco.setCep("01310100");
            endereco.setPrincipal(true);
            endereco.setTipo(TipoEndereco.OUTRO);
            endereco = enderecoRepository.save(endereco);
            enderecoClienteId = endereco.getId();
        } else {
            enderecoClienteId = enderecos.get(0).getId();
        }

        User restauranteUser = testJwtUtil.getOrCreateUser("testrestaurante", ERole.ROLE_RESTAURANTE);
        Restaurante restaurante = restauranteRepository.findByUserId(restauranteUser.getId())
                .orElseGet(() -> {
                    Restaurante r = new Restaurante();
                    r.setUser(restauranteUser);
                    r.setNome("Restaurante Teste");
                    r.setEmail("restaurante@teste.com");
                    r.setTelefone("(11) 88888-8888");
                    return restauranteRepository.save(r);
                });
        restauranteId = restaurante.getId();
        
        var enderecosRestaurante = enderecoRepository.findByRestauranteId(restauranteId);
        if (enderecosRestaurante.isEmpty()) {
            Endereco endereco = new Endereco();
            endereco.setRestaurante(restaurante);
            endereco.setLogradouro("Rua Restaurante");
            endereco.setNumero("456");
            endereco.setBairro("Centro");
            endereco.setCidade("São Paulo");
            endereco.setEstado("SP");
            endereco.setCep("01310100");
            endereco.setPrincipal(true);
            endereco.setTipo(TipoEndereco.OUTRO);
            enderecoRepository.save(endereco);
        }
    }

    @Test
    void deveListarEnderecosCliente() throws Exception {
        mockMvc.perform(get("/api/clientes/{clienteId}/enderecos", clienteId)
                .header("Authorization", "Bearer " + clienteToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveListarEnderecosRestaurante() throws Exception {
        mockMvc.perform(get("/api/restaurantes/{restauranteId}/enderecos", restauranteId)
                .header("Authorization", "Bearer " + restauranteToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveAdicionarEnderecoCliente() throws Exception {
        EnderecoRequestDTO enderecoDTO = criarEnderecoDTO("Rua Nova", "999");

        mockMvc.perform(post("/api/clientes/{clienteId}/enderecos", clienteId)
                .header("Authorization", "Bearer " + clienteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enderecoDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.logradouro").value("Rua Nova"))
                .andExpect(jsonPath("$.numero").value("999"));
    }

    @Test
    void deveAdicionarEnderecoRestaurante() throws Exception {
        EnderecoRequestDTO enderecoDTO = criarEnderecoDTO("Rua Restaurante", "888");

        mockMvc.perform(post("/api/restaurantes/{restauranteId}/enderecos", restauranteId)
                .header("Authorization", "Bearer " + restauranteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enderecoDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.logradouro").value("Rua Restaurante"))
                .andExpect(jsonPath("$.numero").value("888"));
    }

    @Test
    void deveAtualizarEnderecoCliente() throws Exception {
        EnderecoRequestDTO enderecoDTO = criarEnderecoDTO("Rua Atualizada", "777");
        enderecoDTO.setPrincipal(true);

        mockMvc.perform(put("/api/clientes/{clienteId}/enderecos/{enderecoId}", clienteId, enderecoClienteId)
                .header("Authorization", "Bearer " + clienteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(enderecoDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logradouro").value("Rua Atualizada"));
    }

    @Test
    void deveDefinirEnderecoPrincipalCliente() throws Exception {
        EnderecoRequestDTO novoEnderecoDTO = criarEnderecoDTO("Rua Secundaria", "999");
        novoEnderecoDTO.setPrincipal(false);
        Endereco novoEndereco = enderecoService.criarEndereco(novoEnderecoDTO, 
                clienteRepository.findById(clienteId).orElseThrow());

        mockMvc.perform(patch("/api/clientes/{clienteId}/enderecos/{enderecoId}/principal", clienteId, novoEndereco.getId())
                .header("Authorization", "Bearer " + clienteToken))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornarErroAoExcluirUnicoEndereco() throws Exception {
        User userUnico = testJwtUtil.getOrCreateUser("clienteunico", ERole.ROLE_CLIENTE);
        Cliente clienteUnico = clienteRepository.findByUserId(userUnico.getId())
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setUser(userUnico);
                    c.setNome("Cliente Unico");
                    c.setEmail("unico@teste.com");
                    c.setTelefone("(11) 77777-7777");
                    return clienteRepository.save(c);
                });
        
        EnderecoRequestDTO enderecoDTO = criarEnderecoDTO("Rua Unica", "111");
        enderecoDTO.setPrincipal(true);
        Endereco enderecoUnico = enderecoService.criarEndereco(enderecoDTO, clienteUnico);
        
        String tokenUnico = testJwtUtil.generateTokenForUser("clienteunico", ERole.ROLE_CLIENTE);

        mockMvc.perform(delete("/api/clientes/{clienteId}/enderecos/{enderecoId}", clienteUnico.getId(), enderecoUnico.getId())
                .header("Authorization", "Bearer " + tokenUnico))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarForbiddenParaClienteSemPermissao() throws Exception {
        User outroUser = testJwtUtil.getOrCreateUser("outrocliente", ERole.ROLE_CLIENTE);
        Cliente outroCliente = clienteRepository.findByUserId(outroUser.getId())
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setUser(outroUser);
                    c.setNome("Outro Cliente");
                    c.setEmail("outro@teste.com");
                    c.setTelefone("(11) 66666-6666");
                    return clienteRepository.save(c);
                });

        mockMvc.perform(get("/api/clientes/{clienteId}/enderecos", outroCliente.getId())
                .header("Authorization", "Bearer " + clienteToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}

