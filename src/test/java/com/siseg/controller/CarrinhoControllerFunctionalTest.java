package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.carrinho.AplicarCupomRequestDTO;
import com.siseg.dto.carrinho.CarrinhoItemRequestDTO;
import com.siseg.dto.carrinho.CarrinhoResponseDTO;
import com.siseg.model.enumerations.ERole;
import com.siseg.service.CarrinhoService;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarrinhoControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CarrinhoService carrinhoService;

    private String clienteToken;
    private CarrinhoResponseDTO carrinhoResponseDTO;

    @BeforeEach
    void setUp() throws Exception {
        clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);

        carrinhoResponseDTO = new CarrinhoResponseDTO();
        carrinhoResponseDTO.setId(1L);
        carrinhoResponseDTO.setSubtotal(new BigDecimal("50.00"));
        carrinhoResponseDTO.setTotal(new BigDecimal("50.00"));
    }

    @Test
    void deveObterCarrinhoAtivo() throws Exception {
        when(carrinhoService.obterCarrinhoAtivo()).thenReturn(carrinhoResponseDTO);

        mockMvc.perform(get("/api/carrinho")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.subtotal").value(50.00));
    }

    @Test
    void deveAdicionarItemAoCarrinho() throws Exception {
        CarrinhoItemRequestDTO itemDTO = new CarrinhoItemRequestDTO();
        itemDTO.setPratoId(1L);
        itemDTO.setQuantidade(2);

        when(carrinhoService.adicionarItem(any(CarrinhoItemRequestDTO.class))).thenReturn(carrinhoResponseDTO);

        mockMvc.perform(post("/api/carrinho/itens")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void deveAtualizarQuantidadeItem() throws Exception {
        when(carrinhoService.atualizarQuantidade(1L, 3)).thenReturn(carrinhoResponseDTO);

        mockMvc.perform(patch("/api/carrinho/itens/1")
                        .header("Authorization", "Bearer " + clienteToken)
                        .param("quantidade", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deveRemoverItemDoCarrinho() throws Exception {
        when(carrinhoService.removerItem(1L)).thenReturn(carrinhoResponseDTO);

        mockMvc.perform(delete("/api/carrinho/itens/1")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deveAplicarCupomValido() throws Exception {
        AplicarCupomRequestDTO cupomDTO = new AplicarCupomRequestDTO();
        cupomDTO.setCodigo("DESCONTO10");

        when(carrinhoService.aplicarCupom(any(AplicarCupomRequestDTO.class))).thenReturn(carrinhoResponseDTO);

        mockMvc.perform(post("/api/carrinho/cupom")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cupomDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void deveRemoverCupomDoCarrinho() throws Exception {
        when(carrinhoService.removerCupom()).thenReturn(carrinhoResponseDTO);

        mockMvc.perform(delete("/api/carrinho/cupom")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deveLimparCarrinho() throws Exception {
        mockMvc.perform(delete("/api/carrinho")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

