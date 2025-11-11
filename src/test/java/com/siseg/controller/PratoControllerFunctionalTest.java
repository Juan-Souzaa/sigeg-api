package com.siseg.controller;

import com.siseg.dto.prato.PratoRequestDTO;
import com.siseg.dto.prato.PratoResponseDTO;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.ERole;
import com.siseg.service.PratoService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PratoControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @MockBean
    private PratoService pratoService;

    private String restauranteToken;
    private PratoResponseDTO pratoResponseDTO;

    @BeforeEach
    void setUp() throws Exception {
        restauranteToken = testJwtUtil.generateTokenForUser("restaurante", ERole.ROLE_RESTAURANTE);

        pratoResponseDTO = new PratoResponseDTO();
        pratoResponseDTO.setId(1L);
        pratoResponseDTO.setNome("Prato Teste");
        pratoResponseDTO.setPreco(new BigDecimal("25.50"));
        pratoResponseDTO.setCategoria(CategoriaMenu.MAIN);
        pratoResponseDTO.setDisponivel(true);
    }

    @Test
    void deveCriarPrato() throws Exception {
        when(pratoService.criarPrato(eq(1L), any(PratoRequestDTO.class))).thenReturn(pratoResponseDTO);

        mockMvc.perform(multipart("/api/restaurantes/1/pratos")
                        .file("foto", new byte[0])
                        .param("nome", "Prato Teste")
                        .param("descricao", "Descrição")
                        .param("preco", "25.50")
                        .param("categoria", "MAIN")
                        .param("disponivel", "true")
                        .header("Authorization", "Bearer " + restauranteToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Prato Teste"));
    }

    @Test
    void deveAtualizarPrato() throws Exception {
        when(pratoService.atualizarPrato(eq(1L), any(PratoRequestDTO.class))).thenReturn(pratoResponseDTO);

        mockMvc.perform(multipart("/api/restaurantes/1/pratos/1")
                        .file("foto", new byte[0])
                        .param("nome", "Prato Atualizado")
                        .param("preco", "30.00")
                        .param("categoria", "MAIN")
                        .header("Authorization", "Bearer " + restauranteToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void deveAlternarDisponibilidade() throws Exception {
        pratoResponseDTO.setDisponivel(false);

        when(pratoService.alternarDisponibilidade(1L)).thenReturn(pratoResponseDTO);

        mockMvc.perform(patch("/api/restaurantes/1/pratos/1/disponibilidade")
                        .header("Authorization", "Bearer " + restauranteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponivel").value(false));
    }

    @Test
    void deveListarPratosComFiltros() throws Exception {
        Page<PratoResponseDTO> page = new PageImpl<>(List.of(pratoResponseDTO), PageRequest.of(0, 10), 1);

        when(pratoService.listarPorRestaurante(eq(1L), eq(CategoriaMenu.MAIN), eq(true), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/restaurantes/1/pratos")
                        .header("Authorization", "Bearer " + restauranteToken)
                        .param("categoria", "MAIN")
                        .param("disponivel", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deveListarPratosSemFiltros() throws Exception {
        Page<PratoResponseDTO> page = new PageImpl<>(List.of(pratoResponseDTO), PageRequest.of(0, 10), 1);

        when(pratoService.listarPorRestaurante(eq(1L), isNull(), isNull(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/restaurantes/1/pratos")
                        .header("Authorization", "Bearer " + restauranteToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}

