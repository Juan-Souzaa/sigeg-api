package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.cupom.CupomRequestDTO;
import com.siseg.dto.cupom.CupomResponseDTO;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.TipoDesconto;
import com.siseg.service.CupomService;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CupomControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CupomService cupomService;

    private String adminToken;
    private String clienteToken;
    private CupomResponseDTO cupomResponseDTO;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = testJwtUtil.generateTokenForUser("admin", ERole.ROLE_ADMIN);
        clienteToken = testJwtUtil.generateTokenForUser("cliente", ERole.ROLE_CLIENTE);

        cupomResponseDTO = new CupomResponseDTO();
        cupomResponseDTO.setId(1L);
        cupomResponseDTO.setCodigo("DESCONTO10");
        cupomResponseDTO.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupomResponseDTO.setValorDesconto(new BigDecimal("10.00"));
        cupomResponseDTO.setAtivo(true);
    }

    @Test
    void deveCriarCupomComoAdmin() throws Exception {
        CupomRequestDTO requestDTO = new CupomRequestDTO();
        requestDTO.setCodigo("DESCONTO10");
        requestDTO.setTipoDesconto(TipoDesconto.PERCENTUAL);
        requestDTO.setValorDesconto(new BigDecimal("10.00"));
        requestDTO.setValorMinimo(new BigDecimal("50.00"));
        requestDTO.setDataInicio(LocalDate.now());
        requestDTO.setDataFim(LocalDate.now().plusMonths(1));
        requestDTO.setUsosMaximos(100);

        when(cupomService.criarCupom(any(CupomRequestDTO.class))).thenReturn(cupomResponseDTO);

        mockMvc.perform(post("/api/cupons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("DESCONTO10"));
    }

    @Test
    void deveNegarAcessoAoCriarCupomComoCliente() throws Exception {
        CupomRequestDTO requestDTO = new CupomRequestDTO();
        requestDTO.setCodigo("DESCONTO10");
        requestDTO.setTipoDesconto(TipoDesconto.PERCENTUAL);
        requestDTO.setValorDesconto(new BigDecimal("10.00"));
        requestDTO.setValorMinimo(new BigDecimal("50.00"));
        requestDTO.setDataInicio(LocalDate.now());
        requestDTO.setDataFim(LocalDate.now().plusMonths(1));
        requestDTO.setUsosMaximos(100);

        mockMvc.perform(post("/api/cupons")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveBuscarCupomPorCodigo() throws Exception {
        when(cupomService.buscarPorCodigoDTO("DESCONTO10")).thenReturn(cupomResponseDTO);

        mockMvc.perform(get("/api/cupons/DESCONTO10")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("DESCONTO10"));
    }

    @Test
    void deveListarCuponsAtivosComoAdmin() throws Exception {
        Page<CupomResponseDTO> page = new PageImpl<>(List.of(cupomResponseDTO), PageRequest.of(0, 10), 1);

        when(cupomService.listarCuponsAtivos(any())).thenReturn(page);

        mockMvc.perform(get("/api/cupons")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deveDesativarCupomComoAdmin() throws Exception {
        cupomResponseDTO.setAtivo(false);

        when(cupomService.desativarCupom(1L)).thenReturn(cupomResponseDTO);

        mockMvc.perform(patch("/api/cupons/1/desativar")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }
}

