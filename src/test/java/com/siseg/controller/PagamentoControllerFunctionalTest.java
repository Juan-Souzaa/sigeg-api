package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.pagamento.AsaasCustomerResponseDTO;
import com.siseg.dto.pagamento.AsaasPaymentResponseDTO;
import com.siseg.dto.pagamento.AsaasQrCodeResponseDTO;
import com.siseg.dto.pagamento.AsaasRefundResponseDTO;
import com.siseg.dto.pagamento.AsaasWebhookDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.Restaurante;
import com.siseg.model.Pagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PagamentoRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.AsaasService;
import com.siseg.service.AsaasWebhookService;
import com.siseg.service.EnderecoService;
import com.siseg.service.PagamentoService;
import com.siseg.util.SecurityUtils;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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
    private EnderecoService enderecoService;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private PagamentoService pagamentoService;
    
    @SpyBean
    private AsaasWebhookService asaasWebhookService;
    
    @SpyBean
    private AsaasService asaasService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;


    private String clienteToken;
    private String restauranteToken;
    private Cliente cliente;
    private Restaurante restaurante;
    private Pedido pedido;
    private AsaasCustomerResponseDTO asaasCustomerResponse;
    private AsaasPaymentResponseDTO asaasPaymentResponse;
    private AsaasQrCodeResponseDTO asaasQrCodeResponse;

    @BeforeEach
    void setUp() throws Exception {
        pagamentoRepository.deleteAll();
        clienteToken = testJwtUtil.generateTokenForUser("cliente", ERole.ROLE_CLIENTE);

        User user = testJwtUtil.getOrCreateUser("cliente", ERole.ROLE_CLIENTE);
        cliente = clienteRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cliente existente = clienteRepository.findByEmail("cliente@teste.com").orElse(null);
                    if (existente != null) {
                        return existente;
                    }
                    Cliente c = new Cliente();
                    c.setUser(user);
                    c.setNome("Cliente Teste");
                    c.setEmail("cliente@teste.com");
                    c.setTelefone("(11) 99999-9999");
                    Cliente saved = clienteRepository.save(c);
                    
                    // Criar endereço
                    EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
                    enderecoDTO.setLogradouro("Rua Teste");
                    enderecoDTO.setNumero("123");
                    enderecoDTO.setBairro("Centro");
                    enderecoDTO.setCidade("São Paulo");
                    enderecoDTO.setEstado("SP");
                    enderecoDTO.setCep("01310100");
                    enderecoDTO.setPrincipal(true);
                    enderecoService.criarEndereco(enderecoDTO, saved);
                    
                    return saved;
                });

        restaurante = restauranteRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Restaurante r = new Restaurante();
                    r.setNome("Restaurante Teste");
                    r.setEmail("restaurante@teste.com");
                    r.setTelefone("(11) 88888-8888");
                    User restauranteUser = testJwtUtil.getOrCreateUser("restaurante", ERole.ROLE_RESTAURANTE);
                    r.setUser(restauranteUser);
                    Restaurante saved = restauranteRepository.save(r);
                    
                    // Criar endereço
                    EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
                    enderecoDTO.setLogradouro("Rua Restaurante");
                    enderecoDTO.setNumero("456");
                    enderecoDTO.setBairro("Centro");
                    enderecoDTO.setCidade("São Paulo");
                    enderecoDTO.setEstado("SP");
                    enderecoDTO.setCep("01310100");
                    enderecoDTO.setPrincipal(true);
                    enderecoService.criarEndereco(enderecoDTO, saved);
                    
                    return saved;
                });
        
        restauranteToken = testJwtUtil.generateTokenForUser("restaurante", ERole.ROLE_RESTAURANTE);

        com.siseg.model.Endereco enderecoEntrega = enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não possui endereço"));

        pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setStatus(StatusPedido.CREATED);
        pedido.setMetodoPagamento(MetodoPagamento.PIX);
        pedido.setSubtotal(new BigDecimal("90.00"));
        pedido.setTaxaEntrega(new BigDecimal("10.00"));
        pedido.setTotal(new BigDecimal("100.00"));
        pedido.setEnderecoEntrega(enderecoEntrega);
        pedido = pedidoRepository.save(pedido);

        asaasCustomerResponse = new AsaasCustomerResponseDTO();
        asaasCustomerResponse.setId("cus_123456");
        asaasCustomerResponse.setEmail("cliente@teste.com");

        asaasPaymentResponse = new AsaasPaymentResponseDTO();
        asaasPaymentResponse.setId("pay_123456");
        asaasPaymentResponse.setStatus("PENDING");
        asaasPaymentResponse.setValue("100.00");

        asaasQrCodeResponse = new AsaasQrCodeResponseDTO();
        asaasQrCodeResponse.setEncodedImage("iVBORw0KGgoAAAANS");
        asaasQrCodeResponse.setPayload("00020126...");

        mockAsaasService();
    }

    private void mockAsaasService() {
        lenient().doReturn("cus_123456").when(asaasService).buscarOuCriarCliente(any(Cliente.class));
        lenient().doReturn(asaasPaymentResponse).when(asaasService).criarPagamentoPix(any(Pagamento.class), anyString());
        lenient().doReturn(asaasQrCodeResponse).when(asaasService).buscarQrCodePix(anyString());
    }

    @Test
    void deveCriarPagamentoParaPedido() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(cliente.getUser());

            mockMvc.perform(post("/api/pagamentos/pedidos/" + pedido.getId())
                            .header("Authorization", "Bearer " + clienteToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pedidoId").value(pedido.getId()))
                    .andExpect(jsonPath("$.status").exists());
        }
    }

    @Test
    void deveBuscarPagamentoPorPedido() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(cliente.getUser());

            pagamentoService.criarPagamento(pedido.getId(), null, null);

            mockMvc.perform(get("/api/pagamentos/pedidos/" + pedido.getId())
                            .header("Authorization", "Bearer " + clienteToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pedidoId").value(pedido.getId()));
        }
    }

    @Test
    void deveProcessarWebhookValido() throws Exception {
        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setMetodo(MetodoPagamento.PIX);
        pagamento.setStatus(StatusPagamento.PENDING);
        pagamento.setValor(new BigDecimal("100.00"));
        pagamento.setAsaasPaymentId("pay_123456");
        pagamento.setAsaasCustomerId("cus_123456");
        pagamento = pagamentoRepository.save(pagamento);

        AsaasWebhookDTO webhook = new AsaasWebhookDTO();
        webhook.setEvent("PAYMENT_RECEIVED");
        AsaasWebhookDTO.PaymentData paymentData = new AsaasWebhookDTO.PaymentData();
        paymentData.setId("pay_123456");
        webhook.setPayment(paymentData);

        ReflectionTestUtils.setField(asaasWebhookService, "webhookSecret", "test-secret");
        doReturn(true).when(asaasWebhookService).validarAssinatura(anyString(), anyString());

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

        ReflectionTestUtils.setField(asaasWebhookService, "webhookSecret", "test-secret");
        doReturn(false).when(asaasWebhookService).validarAssinatura(anyString(), anyString());

        mockMvc.perform(post("/api/pagamentos/webhook")
                        .header("X-Signature", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Assinatura inválida"));
    }

    @Test
    void deveEstornarPagamentoComSucesso() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(cliente.getUser());

            Pagamento pagamento = new Pagamento();
            pagamento.setPedido(pedido);
            pagamento.setMetodo(MetodoPagamento.PIX);
            pagamento.setStatus(StatusPagamento.PAID);
            pagamento.setValor(new BigDecimal("100.00"));
            pagamento.setAsaasPaymentId("pay_123456");
            pagamento = pagamentoRepository.save(pagamento);

            AsaasRefundResponseDTO refundResponse = new AsaasRefundResponseDTO();
            refundResponse.setId("refund_123456");
            refundResponse.setValue("100.00");
            refundResponse.setStatus("REFUNDED");

            doReturn(refundResponse).when(asaasService).estornarPagamento(anyString(), anyString());

            String requestBody = "{\"motivo\":\"Teste de reembolso\"}";

            mockMvc.perform(post("/api/pagamentos/pedidos/" + pedido.getId() + "/reembolso")
                            .header("Authorization", "Bearer " + restauranteToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoJaReembolsado() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(cliente.getUser());

            Pagamento pagamento = new Pagamento();
            pagamento.setPedido(pedido);
            pagamento.setMetodo(MetodoPagamento.PIX);
            pagamento.setStatus(StatusPagamento.REFUNDED);
            pagamento.setValor(new BigDecimal("100.00"));
            pagamento.setAsaasPaymentId("pay_123456");
            pagamento = pagamentoRepository.save(pagamento);

            String requestBody = "{\"motivo\":\"Teste de reembolso\"}";

            mockMvc.perform(post("/api/pagamentos/pedidos/" + pedido.getId() + "/reembolso")
                            .header("Authorization", "Bearer " + restauranteToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }
}

