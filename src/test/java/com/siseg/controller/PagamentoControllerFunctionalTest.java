package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.pagamento.AsaasWebhookDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.service.PagamentoService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagamentoControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PagamentoService pagamentoService;

    private String clienteToken;
    private PagamentoResponseDTO pagamentoResponseDTO;

    @BeforeEach
    void setUp() throws Exception {
        clienteToken = testJwtUtil.generateTokenForUser("cliente", ERole.ROLE_CLIENTE);

        pagamentoResponseDTO = new PagamentoResponseDTO();
        pagamentoResponseDTO.setId(1L);
        pagamentoResponseDTO.setPedidoId(1L);
        pagamentoResponseDTO.setStatus(StatusPagamento.PENDING);
        pagamentoResponseDTO.setValor(new BigDecimal("100.00"));
    }

    @Test
    void deveCriarPagamentoParaPedido() throws Exception {
        when(pagamentoService.criarPagamento(1L)).thenReturn(pagamentoResponseDTO);

        mockMvc.perform(post("/api/pagamentos/pedidos/1")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void deveBuscarPagamentoPorPedido() throws Exception {
        when(pagamentoService.buscarPagamentoPorPedido(1L)).thenReturn(pagamentoResponseDTO);

        mockMvc.perform(get("/api/pagamentos/pedidos/1")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void deveProcessarWebhookValido() throws Exception {
        AsaasWebhookDTO webhook = new AsaasWebhookDTO();
        webhook.setEvent("PAYMENT_RECEIVED");
        AsaasWebhookDTO.PaymentData paymentData = new AsaasWebhookDTO.PaymentData();
        paymentData.setId("pay_123456");
        webhook.setPayment(paymentData);

        when(pagamentoService.validarWebhookSignature(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/pagamentos/webhook")
                        .header("X-Signature", "valid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processado com sucesso"));
    }

    @Test
    void deveRejeitarWebhookComAssinaturaInvalida() throws Exception {
        AsaasWebhookDTO webhook = new AsaasWebhookDTO();
        webhook.setEvent("PAYMENT_RECEIVED");

        when(pagamentoService.validarWebhookSignature(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/pagamentos/webhook")
                        .header("X-Signature", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Assinatura inválida"));
    }
}

