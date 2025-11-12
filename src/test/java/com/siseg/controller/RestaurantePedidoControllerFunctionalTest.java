package com.siseg.controller;

import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.service.PedidoService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestaurantePedidoControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @MockBean
    private PedidoService pedidoService;

    private String restauranteToken;
    private String clienteToken;
    private Page<PedidoResponseDTO> pedidosPage;

    @BeforeEach
    void setUp() throws Exception {
        try {
            restauranteToken = testJwtUtil.generateTokenForUser("testrestaurante", ERole.ROLE_RESTAURANTE);
            clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
        } catch (Exception e) {
            try {
                Thread.sleep(100);
                restauranteToken = testJwtUtil.generateTokenForUser("testrestaurante", ERole.ROLE_RESTAURANTE);
                clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
            } catch (Exception ex) {
                throw new RuntimeException("Erro ao gerar tokens JWT para testes", ex);
            }
        }

        PedidoResponseDTO pedido1 = new PedidoResponseDTO();
        pedido1.setId(1L);
        pedido1.setClienteId(1L);
        pedido1.setRestauranteId(1L);
        pedido1.setStatus(StatusPedido.PREPARING);
        pedido1.setMetodoPagamento(MetodoPagamento.PIX);
        pedido1.setSubtotal(new BigDecimal("50.00"));
        pedido1.setTaxaEntrega(new BigDecimal("5.00"));
        pedido1.setTotal(new BigDecimal("55.00"));
        pedido1.setEnderecoEntrega("Rua Teste, 123");
        pedido1.setCriadoEm(Instant.now().minusSeconds(86400));

        PedidoResponseDTO pedido2 = new PedidoResponseDTO();
        pedido2.setId(2L);
        pedido2.setClienteId(2L);
        pedido2.setRestauranteId(1L);
        pedido2.setStatus(StatusPedido.CONFIRMED);
        pedido2.setMetodoPagamento(MetodoPagamento.PIX);
        pedido2.setSubtotal(new BigDecimal("30.00"));
        pedido2.setTaxaEntrega(new BigDecimal("5.00"));
        pedido2.setTotal(new BigDecimal("35.00"));
        pedido2.setEnderecoEntrega("Rua Teste, 456");
        pedido2.setCriadoEm(Instant.now());

        pedidosPage = new PageImpl<>(Arrays.asList(pedido1, pedido2), PageRequest.of(0, 10), 2);
    }

    @Test
    void deveListarPedidosRestauranteComSucesso() throws Exception {
        when(pedidoService.listarPedidosRestaurante(isNull(), isNull(), isNull(), any()))
                .thenReturn(pedidosPage);

        mockMvc.perform(get("/api/restaurantes/pedidos/meus-pedidos")
                .header("Authorization", "Bearer " + restauranteToken)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    void deveListarPedidosRestauranteComFiltroStatus() throws Exception {
        Page<PedidoResponseDTO> pedidosFiltrados = new PageImpl<>(
                Arrays.asList(pedidosPage.getContent().get(0)),
                PageRequest.of(0, 10), 1
        );

        when(pedidoService.listarPedidosRestaurante(eq(StatusPedido.PREPARING), isNull(), isNull(), any()))
                .thenReturn(pedidosFiltrados);

        mockMvc.perform(get("/api/restaurantes/pedidos/meus-pedidos")
                .header("Authorization", "Bearer " + restauranteToken)
                .param("status", "PREPARING")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PREPARING"));
    }

    @Test
    void deveListarPedidosRestauranteComFiltroPeriodo() throws Exception {
        Instant dataInicio = Instant.now().minusSeconds(172800);
        Instant dataFim = Instant.now().plusSeconds(86400);

        when(pedidoService.listarPedidosRestaurante(isNull(), eq(dataInicio), eq(dataFim), any()))
                .thenReturn(pedidosPage);

        mockMvc.perform(get("/api/restaurantes/pedidos/meus-pedidos")
                .header("Authorization", "Bearer " + restauranteToken)
                .param("dataInicio", dataInicio.toString())
                .param("dataFim", dataFim.toString())
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void deveRetornarForbiddenQuandoUsuarioNaoERestaurante() throws Exception {
        mockMvc.perform(get("/api/restaurantes/pedidos/meus-pedidos")
                .header("Authorization", "Bearer " + clienteToken)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}

