package com.siseg.service;

import com.siseg.dto.pagamento.*;
import com.siseg.exception.PaymentGatewayException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Pagamento;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.mapper.PagamentoMapper;
import com.siseg.repository.PagamentoRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.PagamentoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceUnitTest {

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PagamentoMapper pagamentoMapper;

    @Mock
    private PagamentoValidator pagamentoValidator;

    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Pedido pedido;
    private Cliente cliente;
    private User user;
    private Pagamento pagamento;
    private PagamentoResponseDTO pagamentoResponseDTO;
    private AsaasPaymentResponseDTO asaasPaymentResponse;
    private AsaasQrCodeResponseDTO asaasQrCodeResponse;
    private AsaasCustomerResponseDTO asaasCustomerResponse;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagamentoService, "webhookSecret", "test-secret");
        ReflectionTestUtils.setField(pagamentoService, "webClient", webClient);

        user = new User();
        user.setId(1L);
        user.setUsername("cliente@teste.com");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente@teste.com");
        cliente.setUser(user);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.CREATED);
        pedido.setMetodoPagamento(MetodoPagamento.PIX);
        pedido.setTotal(new BigDecimal("100.00"));
        pedido.setTroco(null);

        pagamento = new Pagamento();
        pagamento.setId(1L);
        pagamento.setPedido(pedido);
        pagamento.setMetodo(MetodoPagamento.PIX);
        pagamento.setValor(new BigDecimal("100.00"));
        pagamento.setStatus(StatusPagamento.PENDING);

        pagamentoResponseDTO = new PagamentoResponseDTO();
        pagamentoResponseDTO.setId(1L);
        pagamentoResponseDTO.setPedidoId(1L);
        pagamentoResponseDTO.setStatus(StatusPagamento.PENDING);

        asaasPaymentResponse = new AsaasPaymentResponseDTO();
        asaasPaymentResponse.setId("pay_123456");
        asaasPaymentResponse.setCustomer("cus_123456");
        asaasPaymentResponse.setStatus("AUTHORIZED");

        asaasQrCodeResponse = new AsaasQrCodeResponseDTO();
        asaasQrCodeResponse.setPayload("00020126580014BR.GOV.BCB.PIX");
        asaasQrCodeResponse.setEncodedImage("data:image/png;base64,iVBORw0KGgoAAAANS");

        asaasCustomerResponse = new AsaasCustomerResponseDTO();
        asaasCustomerResponse.setId("cus_123456");
        asaasCustomerResponse.setEmail("cliente@teste.com");
    }

    @Test
    void deveCriarPagamentoPixComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doNothing().when(pagamentoValidator).validateStatusPedido(any(Pedido.class));
            when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(modelMapper.map(any(Pagamento.class), eq(PagamentoResponseDTO.class))).thenReturn(pagamentoResponseDTO);

            mockWebClientForPix();

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L);

            assertNotNull(result);
            verify(pagamentoRepository, times(1)).save(any(Pagamento.class));
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoEncontrado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> pagamentoService.criarPagamento(1L));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoEmStatusInvalido() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setStatus(StatusPedido.DELIVERED);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doThrow(new RuntimeException("Pedido já foi processado"))
                    .when(pagamentoValidator).validateStatusPedido(any(Pedido.class));

            assertThrows(RuntimeException.class, 
                    () -> pagamentoService.criarPagamento(1L));
        }
    }


    @Test
    void deveProcessarPagamentoPixComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doNothing().when(pagamentoValidator).validateStatusPedido(any(Pedido.class));
            when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(modelMapper.map(any(Pagamento.class), eq(PagamentoResponseDTO.class))).thenReturn(pagamentoResponseDTO);

            mockWebClientForPix();

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L);

            assertNotNull(result);
            verify(pagamentoRepository, times(1)).save(any(Pagamento.class));
        }
    }

    @Test
    void deveLancarExcecaoQuandoErroNaApiAsaas() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doNothing().when(pagamentoValidator).validateStatusPedido(any(Pedido.class));
            lenient().when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });

            lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
            lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            lenient().when(responseSpec.bodyToMono(AsaasCustomerResponseDTO.class))
                    .thenReturn(Mono.error(new RuntimeException("Connection error")));

            assertThrows(PaymentGatewayException.class, 
                    () -> pagamentoService.criarPagamento(1L));
        }
    }

    @Test
    void deveIncluirQrCodeAoCriarPagamentoPix() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doNothing().when(pagamentoValidator).validateStatusPedido(any(Pedido.class));
            when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(modelMapper.map(any(Pagamento.class), eq(PagamentoResponseDTO.class))).thenReturn(pagamentoResponseDTO);

            mockWebClientForPix();

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L);

            assertNotNull(result);
            verify(pagamentoRepository, times(1)).save(argThat(p -> 
                p.getQrCode() != null && p.getAsaasPaymentId() != null));
        }
    }

    @Test
    void deveProcessarWebhookComPagamentoConfirmado() {
        AsaasWebhookDTO webhook = new AsaasWebhookDTO();
        webhook.setEvent("PAYMENT_RECEIVED");
        AsaasWebhookDTO.PaymentData paymentData = new AsaasWebhookDTO.PaymentData();
        paymentData.setId("pay_123456");
        paymentData.setStatus("RECEIVED");
        webhook.setPayment(paymentData);

        pagamento.setAsaasPaymentId("pay_123456");
        pagamento.setStatus(StatusPagamento.AUTHORIZED);

        when(pagamentoRepository.findByAsaasPaymentId("pay_123456")).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        pagamentoService.processarWebhook(webhook);

        assertEquals(StatusPagamento.PAID, pagamento.getStatus());
        assertEquals(StatusPedido.CONFIRMED, pedido.getStatus());
        verify(pagamentoRepository, times(1)).save(pagamento);
        verify(pedidoRepository, times(1)).save(pedido);
    }

    @Test
    void deveIgnorarWebhookQuandoEventoNaoEhPaymentReceived() {
        AsaasWebhookDTO webhook = new AsaasWebhookDTO();
        webhook.setEvent("PAYMENT_OVERDUE");

        pagamentoService.processarWebhook(webhook);

        verify(pagamentoRepository, never()).findByAsaasPaymentId(anyString());
    }

    @Test
    void deveLancarExcecaoQuandoAssinaturaInvalida() {
        boolean result = pagamentoService.validarWebhookSignature("invalid-signature", "payload");

        assertFalse(result);
    }

    @Test
    void deveValidarAssinaturaValida() {
        ReflectionTestUtils.setField(pagamentoService, "webhookSecret", "test-secret");
        boolean result = pagamentoService.validarWebhookSignature("test-secret", "payload");

        assertTrue(result);
    }

    @Test
    void deveBuscarPagamentoPorPedidoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pagamentoRepository.findByPedidoId(1L)).thenReturn(Optional.of(pagamento));
            when(modelMapper.map(any(Pagamento.class), eq(PagamentoResponseDTO.class))).thenReturn(pagamentoResponseDTO);

            PagamentoResponseDTO result = pagamentoService.buscarPagamentoPorPedido(1L);

            assertNotNull(result);
            verify(pagamentoRepository, times(1)).findByPedidoId(1L);
        }
    }

    @Test
    void deveProcessarPagamentoComCartaoCreditoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setMetodoPagamento(MetodoPagamento.CREDIT_CARD);
            pagamento.setMetodo(MetodoPagamento.CREDIT_CARD);

            CartaoCreditoRequestDTO cartaoDTO = new CartaoCreditoRequestDTO();
            cartaoDTO.setNumero("4111111111111111");
            cartaoDTO.setNomeTitular("Cliente Teste");
            cartaoDTO.setValidade("12/25");
            cartaoDTO.setCvv("123");

            asaasPaymentResponse.setStatus("CONFIRMED");

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pagamentoRepository.save(any(Pagamento.class))).thenReturn(pagamento);
            when(modelMapper.map(any(Pagamento.class), eq(PagamentoResponseDTO.class))).thenReturn(pagamentoResponseDTO);
            lenient().doNothing().when(pagamentoValidator).validateStatusPedido(any(Pedido.class));

            mockWebClientForCartao();

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L, cartaoDTO);

            assertNotNull(result);
            verify(pagamentoRepository, times(1)).save(any(Pagamento.class));
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }
    }

    @Test
    void deveLancarExcecaoQuandoCartaoNaoFornecidoParaPagamentoCartao() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setMetodoPagamento(MetodoPagamento.CREDIT_CARD);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            lenient().doNothing().when(pagamentoValidator).validateStatusPedido(any(Pedido.class));

            assertThrows(IllegalArgumentException.class, 
                    () -> pagamentoService.criarPagamento(1L, null));
        }
    }

    @SuppressWarnings("unchecked")
    private void mockWebClientForPix() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AsaasPaymentResponseDTO.class)).thenReturn(Mono.just(asaasPaymentResponse));
        when(responseSpec.bodyToMono(AsaasCustomerResponseDTO.class)).thenReturn(Mono.just(asaasCustomerResponse));
        when(responseSpec.bodyToMono(AsaasQrCodeResponseDTO.class)).thenReturn(Mono.just(asaasQrCodeResponse));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
    }

    @SuppressWarnings("unchecked")
    private void mockWebClientForCartao() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AsaasPaymentResponseDTO.class)).thenReturn(Mono.just(asaasPaymentResponse));
        when(responseSpec.bodyToMono(AsaasCustomerResponseDTO.class)).thenReturn(Mono.just(asaasCustomerResponse));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
    }

}

