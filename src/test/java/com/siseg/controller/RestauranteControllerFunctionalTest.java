package com.siseg.controller;

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

    private String userToken;
    private String adminToken;

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
        String json = """
                {
                    "nome": "Restaurante de Teste",
                    "email": "teste@restaurante.com",
                    "telefone": "(11) 99999-9999",
                    "endereco": "Rua Teste, 123"
                }
                """;

        mockMvc.perform(post("/api/restaurantes")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Restaurante de Teste"))
                .andExpect(jsonPath("$.email").value("teste@restaurante.com"))
                .andExpect(jsonPath("$.telefone").value("(11) 99999-9999"))
                .andExpect(jsonPath("$.endereco").value("Rua Teste, 123"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void deveRetornarErro422AoCriarRestauranteComCamposObrigatoriosFaltando() throws Exception {
        String json = """
                {
                    "email": "teste@restaurante.com",
                    "telefone": "(11) 99999-9999"
                }
                """;

        mockMvc.perform(post("/api/restaurantes")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
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
        String json = """
                {
                    "nome": "Restaurante para Listagem",
                    "email": "listagem@restaurante.com",
                    "telefone": "(11) 88888-8888",
                    "endereco": "Rua Listagem, 456"
                }
                """;

        mockMvc.perform(post("/api/restaurantes")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
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
        String json = """
                {
                    "nome": "Restaurante para Busca",
                    "email": "busca@restaurante.com",
                    "telefone": "(11) 77777-7777",
                    "endereco": "Rua Busca, 789"
                }
                """;

        String response = mockMvc.perform(post("/api/restaurantes")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
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
        String json = """
                {
                    "nome": "Restaurante para Aprovar",
                    "email": "aprovar@restaurante.com",
                    "telefone": "(11) 66666-6666",
                    "endereco": "Rua Aprovar, 101"
                }
                """;

        String response = mockMvc.perform(post("/api/restaurantes")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
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
        String json = """
                {
                    "nome": "Restaurante para Rejeitar",
                    "email": "rejeitar@restaurante.com",
                    "telefone": "(11) 55555-5555",
                    "endereco": "Rua Rejeitar, 202"
                }
                """;

        String response = mockMvc.perform(post("/api/restaurantes")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
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
}
