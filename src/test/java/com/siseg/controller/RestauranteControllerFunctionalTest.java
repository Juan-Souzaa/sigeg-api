package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.restaurante.RestauranteRequestDTO;
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
class RestauranteControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    private EnderecoRequestDTO criarEnderecoDTO(String logradouro, String numero) {
        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro(logradouro);
        enderecoDTO.setNumero(numero);
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(true);
        return enderecoDTO;
    }

    @BeforeEach
    void setUp() {
        // Gera tokens JWT válidos para os testes
        // Usa retry em caso de erro na criação do schema
        try {
            userToken = testJwtUtil.generateUserToken();
            adminToken = testJwtUtil.generateAdminToken();
        } catch (Exception e) {
            // Se falhar, tenta novamente após um pequeno delay
            try {
                Thread.sleep(100);
                userToken = testJwtUtil.generateUserToken();
                adminToken = testJwtUtil.generateAdminToken();
            } catch (Exception ex) {
                throw new RuntimeException("Erro ao gerar tokens JWT para testes", ex);
            }
        }
    }

    @Test
    void deveCriarRestauranteComSucesso() throws Exception {
        RestauranteRequestDTO requestDTO = new RestauranteRequestDTO();
        requestDTO.setNome("Restaurante de Teste");
        requestDTO.setEmail("teste@restaurante.com");
        requestDTO.setTelefone("(11) 99999-9999");
        requestDTO.setPassword("123456");
        requestDTO.setEndereco(criarEnderecoDTO("Rua Teste", "123"));

        mockMvc.perform(post("/api/restaurantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Restaurante de Teste"))
                .andExpect(jsonPath("$.email").value("teste@restaurante.com"))
                .andExpect(jsonPath("$.telefone").value("(11) 99999-9999"))
                .andExpect(jsonPath("$.endereco").exists()) // Endereço será formatado como string
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void deveRetornarErro422AoCriarRestauranteComCamposObrigatoriosFaltando() throws Exception {
        RestauranteRequestDTO requestDTO = new RestauranteRequestDTO();
        requestDTO.setEmail("teste@restaurante.com");
        requestDTO.setTelefone("(11) 99999-9999");

        mockMvc.perform(post("/api/restaurantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void deveRetornarNotFoundAoBuscarRestauranteInexistente() throws Exception {
        mockMvc.perform(get("/api/restaurantes/9999")
                .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void deveListarRestaurantesComPaginacao() throws Exception {
        // Primeiro cria um restaurante
        RestauranteRequestDTO requestDTO = new RestauranteRequestDTO();
        requestDTO.setNome("Restaurante para Listagem");
        requestDTO.setEmail("listagem@restaurante.com");
        requestDTO.setTelefone("(11) 88888-8888");
        requestDTO.setPassword("123456");
        requestDTO.setEndereco(criarEnderecoDTO("Rua Listagem", "456"));

        mockMvc.perform(post("/api/restaurantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk());

        // Depois lista os restaurantes
        mockMvc.perform(get("/api/restaurantes?page=0&size=10&sort=nome,asc")
                .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void deveBuscarRestaurantePorIdComSucesso() throws Exception {
        // Primeiro cria um restaurante
        RestauranteRequestDTO requestDTO = new RestauranteRequestDTO();
        requestDTO.setNome("Restaurante para Busca");
        requestDTO.setEmail("busca@restaurante.com");
        requestDTO.setTelefone("(11) 77777-7777");
        requestDTO.setPassword("123456");
        requestDTO.setEndereco(criarEnderecoDTO("Rua Busca", "789"));

        String response = mockMvc.perform(post("/api/restaurantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extrai o ID da resposta JSON
        String id = response.substring(response.indexOf("\"id\":") + 5, response.indexOf(","));
        id = id.trim();

        // Busca o restaurante por ID
        mockMvc.perform(get("/api/restaurantes/{id}", id)
                .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Restaurante para Busca"))
                .andExpect(jsonPath("$.email").value("busca@restaurante.com"));
    }

    @Test
    void deveAprovarRestauranteComSucesso() throws Exception {
        // Primeiro cria um restaurante
        RestauranteRequestDTO requestDTO = new RestauranteRequestDTO();
        requestDTO.setNome("Restaurante para Aprovar");
        requestDTO.setEmail("aprovar@restaurante.com");
        requestDTO.setTelefone("(11) 66666-6666");
        requestDTO.setPassword("123456");
        requestDTO.setEndereco(criarEnderecoDTO("Rua Aprovar", "101"));

        String response = mockMvc.perform(post("/api/restaurantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extrai o ID da resposta JSON
        String id = response.substring(response.indexOf("\"id\":") + 5, response.indexOf(","));
        id = id.trim();

        // Aprova o restaurante (requer autenticação de admin)
        mockMvc.perform(patch("/api/restaurantes/{id}/aprovar", id)
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void deveRejeitarRestauranteComSucesso() throws Exception {
        // Primeiro cria um restaurante
        RestauranteRequestDTO requestDTO = new RestauranteRequestDTO();
        requestDTO.setNome("Restaurante para Rejeitar");
        requestDTO.setEmail("rejeitar@restaurante.com");
        requestDTO.setTelefone("(11) 55555-5555");
        requestDTO.setPassword("123456");
        requestDTO.setEndereco(criarEnderecoDTO("Rua Rejeitar", "202"));

        String response = mockMvc.perform(post("/api/restaurantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extrai o ID da resposta JSON
        String id = response.substring(response.indexOf("\"id\":") + 5, response.indexOf(","));
        id = id.trim();

        // Rejeita o restaurante (requer autenticação de admin)
        mockMvc.perform(patch("/api/restaurantes/{id}/rejeitar", id)
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void deveRetornarForbiddenQuandoUsuarioTentaAprovarRestauranteSemSerAdmin() throws Exception {
        // Primeiro cria um restaurante
        RestauranteRequestDTO requestDTO = new RestauranteRequestDTO();
        requestDTO.setNome("Restaurante para Teste Admin");
        requestDTO.setEmail("admin-test@restaurante.com");
        requestDTO.setTelefone("(11) 44444-4444");
        requestDTO.setPassword("123456");
        requestDTO.setEndereco(criarEnderecoDTO("Rua Admin Test", "303"));

        String response = mockMvc.perform(post("/api/restaurantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.substring(response.indexOf("\"id\":") + 5, response.indexOf(","));
        id = id.trim();

        // Tenta aprovar como usuário comum (deve retornar 403)
        mockMvc.perform(patch("/api/restaurantes/{id}/aprovar", id)
                .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
