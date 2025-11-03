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
class EntregadorControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        try {
            userToken = testJwtUtil.generateUserToken();
            adminToken = testJwtUtil.generateAdminToken();
        } catch (Exception e) {
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
    void deveCriarEntregadorComSucesso() throws Exception {
        String json = """
                {
                    "nome": "Entregador de Teste",
                    "email": "entregador.teste@teste.com",
                    "cpf": "12345678901",
                    "telefone": "(11) 99999-9999",
                    "tipoVeiculo": "MOTO",
                    "placaVeiculo": "ABC1234",
                    "latitude": -23.5505,
                    "longitude": -46.6333,
                    "password": "senha123"
                }
                """;

        mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Entregador de Teste"))
                .andExpect(jsonPath("$.email").value("entregador.teste@teste.com"))
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.tipoVeiculo").value("MOTO"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void deveRetornarErro422AoCriarEntregadorComCamposObrigatoriosFaltando() throws Exception {
        String json = """
                {
                    "email": "entregador@teste.com",
                    "telefone": "(11) 99999-9999"
                }
                """;

        mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void deveRetornarNotFoundAoBuscarEntregadorInexistente() throws Exception {
        mockMvc.perform(get("/api/entregadores/9999")
                .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void deveListarEntregadoresComPaginacao() throws Exception {
        // Primeiro cria um entregador
        String json = """
                {
                    "nome": "Entregador para Listagem",
                    "email": "listagem.entregador@teste.com",
                    "cpf": "98765432109",
                    "telefone": "(11) 88888-8888",
                    "tipoVeiculo": "CARRO",
                    "placaVeiculo": "XYZ9876",
                    "latitude": -23.5505,
                    "longitude": -46.6333,
                    "password": "senha123"
                }
                """;

        mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        // Depois lista os entregadores
        mockMvc.perform(get("/api/entregadores?page=0&size=10&sort=nome,asc")
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void deveBuscarEntregadorPorIdComSucesso() throws Exception {
        // Primeiro cria um entregador
        String json = """
                {
                    "nome": "Entregador para Busca",
                    "email": "busca.entregador@teste.com",
                    "cpf": "11122233344",
                    "telefone": "(11) 77777-7777",
                    "tipoVeiculo": "BICICLETA",
                    "placaVeiculo": "BIKE123",
                    "latitude": -23.5505,
                    "longitude": -46.6333,
                    "password": "senha123"
                }
                """;

        String response = mockMvc.perform(post("/api/entregadores")
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

        // Busca o entregador por ID
        mockMvc.perform(get("/api/entregadores/{id}", id)
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Entregador para Busca"))
                .andExpect(jsonPath("$.email").value("busca.entregador@teste.com"));
    }

    @Test
    void deveAprovarEntregadorComSucesso() throws Exception {
        // Primeiro cria um entregador
        String json = """
                {
                    "nome": "Entregador para Aprovar",
                    "email": "aprovar.entregador@teste.com",
                    "cpf": "55566677788",
                    "telefone": "(11) 66666-6666",
                    "tipoVeiculo": "MOTO",
                    "placaVeiculo": "APR1234",
                    "latitude": -23.5505,
                    "longitude": -46.6333,
                    "password": "senha123"
                }
                """;

        String response = mockMvc.perform(post("/api/entregadores")
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

        // Aprova o entregador (requer autenticação de admin)
        mockMvc.perform(patch("/api/entregadores/{id}/aprovar", id)
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void deveRejeitarEntregadorComSucesso() throws Exception {
        // Primeiro cria um entregador
        String json = """
                {
                    "nome": "Entregador para Rejeitar",
                    "email": "rejeitar.entregador@teste.com",
                    "cpf": "33344455566",
                    "telefone": "(11) 55555-5555",
                    "tipoVeiculo": "CARRO",
                    "placaVeiculo": "REJ1234",
                    "latitude": -23.5505,
                    "longitude": -46.6333,
                    "password": "senha123"
                }
                """;

        String response = mockMvc.perform(post("/api/entregadores")
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

        // Rejeita o entregador (requer autenticação de admin)
        mockMvc.perform(patch("/api/entregadores/{id}/rejeitar", id)
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void deveRetornarForbiddenQuandoUsuarioTentaAprovarEntregadorSemSerAdmin() throws Exception {
        // Primeiro cria um entregador
        String json = """
                {
                    "nome": "Entregador para Teste Admin",
                    "email": "admin.test.entregador@teste.com",
                    "cpf": "99988877766",
                    "telefone": "(11) 44444-4444",
                    "tipoVeiculo": "MOTO",
                    "placaVeiculo": "ADM1234",
                    "latitude": -23.5505,
                    "longitude": -46.6333,
                    "password": "senha123"
                }
                """;

        String response = mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.substring(response.indexOf("\"id\":") + 5, response.indexOf(","));
        id = id.trim();

        // Tenta aprovar como usuário comum (deve retornar 403)
        mockMvc.perform(patch("/api/entregadores/{id}/aprovar", id)
                .header("Authorization", "Bearer " + userToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void deveListarEntregadoresPorStatus() throws Exception {
        // Primeiro cria um entregador
        String json = """
                {
                    "nome": "Entregador Status Test",
                    "email": "status.entregador@teste.com",
                    "cpf": "77788899900",
                    "telefone": "(11) 33333-3333",
                    "tipoVeiculo": "BICICLETA",
                    "placaVeiculo": "STS1234",
                    "latitude": -23.5505,
                    "longitude": -46.6333,
                    "password": "senha123"
                }
                """;

        mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        // Lista entregadores por status (requer admin)
        mockMvc.perform(get("/api/entregadores/status/PENDING_APPROVAL?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists());
    }
}

