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
class ClientePedidoControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @MockBean
    private PedidoService pedidoService;

    private String clienteToken;
    private Page<PedidoResponseDTO> pedidosPage;

    @BeforeEach
    void setUp() throws Exception {
        try {
            clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
        } catch (Exception e) {
            try {
                Thread.sleep(100);
                clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
            } catch (Exception ex) {
                throw new RuntimeException("Erro ao gerar token JWT para testes", ex);
            }
        }

        PedidoResponseDTO pedido1 = new PedidoResponseDTO();
        pedido1.setId(1L);
        pedido1.setClienteId(1L);
        pedido1.setRestauranteId(1L);
        pedido1.setStatus(StatusPedido.DELIVERED);
        pedido1.setMetodoPagamento(MetodoPagamento.PIX);
        pedido1.setSubtotal(new BigDecimal("50.00"));
        pedido1.setTaxaEntrega(new BigDecimal("5.00"));
        pedido1.setTotal(new BigDecimal("55.00"));
        pedido1.setEnderecoEntrega("Rua Teste, 123");
        pedido1.setCriadoEm(Instant.now().minusSeconds(86400));

        PedidoResponseDTO pedido2 = new PedidoResponseDTO();
        pedido2.setId(2L);
        pedido2.setClienteId(1L);
        pedido2.setRestauranteId(1L);
        pedido2.setStatus(StatusPedido.PREPARING);
        pedido2.setMetodoPagamento(MetodoPagamento.PIX);
        pedido2.setSubtotal(new BigDecimal("30.00"));
        pedido2.setTaxaEntrega(new BigDecimal("5.00"));
        pedido2.setTotal(new BigDecimal("35.00"));
        pedido2.setEnderecoEntrega("Rua Teste, 123");
        pedido2.setCriadoEm(Instant.now());

        pedidosPage = new PageImpl<>(Arrays.asList(pedido1, pedido2), PageRequest.of(0, 10), 2);
    }

    @Test
    void deveListarMeusPedidosSemFiltros() throws Exception {
        when(pedidoService.listarMeusPedidos(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(pedidosPage);

        mockMvc.perform(get("/api/pedidos/meus-pedidos")
                .header("Authorization", "Bearer " + clienteToken)
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    void deveListarMeusPedidosComFiltroStatus() throws Exception {
        Page<PedidoResponseDTO> pedidosFiltrados = new PageImpl<>(
                Arrays.asList(pedidosPage.getContent().get(0)),
                PageRequest.of(0, 10), 1
        );

        when(pedidoService.listarMeusPedidos(eq(StatusPedido.DELIVERED), isNull(), isNull(), isNull(), any()))
                .thenReturn(pedidosFiltrados);

        mockMvc.perform(get("/api/pedidos/meus-pedidos")
                .header("Authorization", "Bearer " + clienteToken)
                .param("status", "DELIVERED")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("DELIVERED"));
    }

    @Test
    void deveListarMeusPedidosComFiltroRestaurante() throws Exception {
        when(pedidoService.listarMeusPedidos(isNull(), isNull(), isNull(), eq(1L), any()))
                .thenReturn(pedidosPage);

        mockMvc.perform(get("/api/pedidos/meus-pedidos")
                .header("Authorization", "Bearer " + clienteToken)
                .param("restauranteId", "1")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void deveListarMeusPedidosComFiltroData() throws Exception {
        Instant dataInicio = Instant.now().minusSeconds(172800);
        Instant dataFim = Instant.now().plusSeconds(86400);

        when(pedidoService.listarMeusPedidos(isNull(), eq(dataInicio), eq(dataFim), isNull(), any()))
                .thenReturn(pedidosPage);

        mockMvc.perform(get("/api/pedidos/meus-pedidos")
                .header("Authorization", "Bearer " + clienteToken)
                .param("dataInicio", dataInicio.toString())
                .param("dataFim", dataFim.toString())
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}

