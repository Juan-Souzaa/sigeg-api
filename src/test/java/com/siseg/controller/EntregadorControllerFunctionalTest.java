package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.entregador.EntregadorRequestDTO;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

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

    @Autowired
    private ObjectMapper objectMapper;

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
        EntregadorRequestDTO requestDTO = new EntregadorRequestDTO();
        requestDTO.setNome("Entregador de Teste");
        requestDTO.setEmail("entregador.teste@teste.com");
        requestDTO.setCpf("12345678901");
        requestDTO.setTelefone("(11) 99999-9999");
        requestDTO.setTipoVeiculo(TipoVeiculo.MOTO);
        requestDTO.setPlacaVeiculo("ABC1234");
        requestDTO.setLatitude(new BigDecimal("-23.5505"));
        requestDTO.setLongitude(new BigDecimal("-46.6333"));
        requestDTO.setPassword("senha123");

        mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
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
        EntregadorRequestDTO requestDTO = new EntregadorRequestDTO();
        requestDTO.setEmail("entregador@teste.com");
        requestDTO.setTelefone("(11) 99999-9999");

        mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
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
        EntregadorRequestDTO requestDTO = new EntregadorRequestDTO();
        requestDTO.setNome("Entregador para Listagem");
        requestDTO.setEmail("listagem.entregador@teste.com");
        requestDTO.setCpf("98765432109");
        requestDTO.setTelefone("(11) 88888-8888");
        requestDTO.setTipoVeiculo(TipoVeiculo.CARRO);
        requestDTO.setPlacaVeiculo("XYZ9876");
        requestDTO.setLatitude(new BigDecimal("-23.5505"));
        requestDTO.setLongitude(new BigDecimal("-46.6333"));
        requestDTO.setPassword("senha123");

        mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
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
        EntregadorRequestDTO requestDTO = new EntregadorRequestDTO();
        requestDTO.setNome("Entregador para Busca");
        requestDTO.setEmail("busca.entregador@teste.com");
        requestDTO.setCpf("11122233344");
        requestDTO.setTelefone("(11) 77777-7777");
        requestDTO.setTipoVeiculo(TipoVeiculo.BICICLETA);
        requestDTO.setPlacaVeiculo("BIKE123");
        requestDTO.setLatitude(new BigDecimal("-23.5505"));
        requestDTO.setLongitude(new BigDecimal("-46.6333"));
        requestDTO.setPassword("senha123");

        String response = mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
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
        EntregadorRequestDTO requestDTO = new EntregadorRequestDTO();
        requestDTO.setNome("Entregador para Aprovar");
        requestDTO.setEmail("aprovar.entregador@teste.com");
        requestDTO.setCpf("55566677788");
        requestDTO.setTelefone("(11) 66666-6666");
        requestDTO.setTipoVeiculo(TipoVeiculo.MOTO);
        requestDTO.setPlacaVeiculo("APR1234");
        requestDTO.setLatitude(new BigDecimal("-23.5505"));
        requestDTO.setLongitude(new BigDecimal("-46.6333"));
        requestDTO.setPassword("senha123");

        String response = mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
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
        EntregadorRequestDTO requestDTO = new EntregadorRequestDTO();
        requestDTO.setNome("Entregador para Rejeitar");
        requestDTO.setEmail("rejeitar.entregador@teste.com");
        requestDTO.setCpf("33344455566");
        requestDTO.setTelefone("(11) 55555-5555");
        requestDTO.setTipoVeiculo(TipoVeiculo.CARRO);
        requestDTO.setPlacaVeiculo("REJ1234");
        requestDTO.setLatitude(new BigDecimal("-23.5505"));
        requestDTO.setLongitude(new BigDecimal("-46.6333"));
        requestDTO.setPassword("senha123");

        String response = mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
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
        EntregadorRequestDTO requestDTO = new EntregadorRequestDTO();
        requestDTO.setNome("Entregador para Teste Admin");
        requestDTO.setEmail("admin.test.entregador@teste.com");
        requestDTO.setCpf("99988877766");
        requestDTO.setTelefone("(11) 44444-4444");
        requestDTO.setTipoVeiculo(TipoVeiculo.MOTO);
        requestDTO.setPlacaVeiculo("ADM1234");
        requestDTO.setLatitude(new BigDecimal("-23.5505"));
        requestDTO.setLongitude(new BigDecimal("-46.6333"));
        requestDTO.setPassword("senha123");

        String response = mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
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
        EntregadorRequestDTO requestDTO = new EntregadorRequestDTO();
        requestDTO.setNome("Entregador Status Test");
        requestDTO.setEmail("status.entregador@teste.com");
        requestDTO.setCpf("77788899900");
        requestDTO.setTelefone("(11) 33333-3333");
        requestDTO.setTipoVeiculo(TipoVeiculo.BICICLETA);
        requestDTO.setPlacaVeiculo("STS1234");
        requestDTO.setLatitude(new BigDecimal("-23.5505"));
        requestDTO.setLongitude(new BigDecimal("-46.6333"));
        requestDTO.setPassword("senha123");

        mockMvc.perform(post("/api/entregadores")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
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

