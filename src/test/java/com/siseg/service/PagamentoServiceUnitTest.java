package com.siseg.service;

import com.siseg.dto.pagamento.AsaasCustomerResponseDTO;
import com.siseg.dto.pagamento.AsaasPaymentResponseDTO;
import com.siseg.dto.pagamento.AsaasQrCodeResponseDTO;
import com.siseg.dto.pagamento.AsaasRefundResponseDTO;
import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
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
    private AsaasService asaasService;

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

            when(asaasService.buscarOuCriarCliente(any(Cliente.class))).thenReturn("cus_123456");
            when(asaasService.criarPagamentoPix(any(Pagamento.class), anyString())).thenReturn(asaasPaymentResponse);
            when(asaasService.buscarQrCodePix(anyString())).thenReturn(asaasQrCodeResponse);

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L, null, null);

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
                    () -> pagamentoService.criarPagamento(1L, null, null));
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
                    () -> pagamentoService.criarPagamento(1L, null, null));
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

            when(asaasService.buscarOuCriarCliente(any(Cliente.class))).thenReturn("cus_123456");
            when(asaasService.criarPagamentoPix(any(Pagamento.class), anyString())).thenReturn(asaasPaymentResponse);
            when(asaasService.buscarQrCodePix(anyString())).thenReturn(asaasQrCodeResponse);

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L, null, null);

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

            when(asaasService.buscarOuCriarCliente(any(Cliente.class)))
                    .thenThrow(new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet."));

            assertThrows(PaymentGatewayException.class, 
                    () -> pagamentoService.criarPagamento(1L, null, null));
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

            when(asaasService.buscarOuCriarCliente(any(Cliente.class))).thenReturn("cus_123456");
            when(asaasService.criarPagamentoPix(any(Pagamento.class), anyString())).thenReturn(asaasPaymentResponse);
            when(asaasService.buscarQrCodePix(anyString())).thenReturn(asaasQrCodeResponse);

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L, null, null);

            assertNotNull(result);
            verify(pagamentoRepository, times(1)).save(argThat(p -> 
                p.getQrCode() != null && p.getAsaasPaymentId() != null));
        }
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

            when(asaasService.buscarOuCriarCliente(any(Cliente.class))).thenReturn("cus_123456");
            when(asaasService.criarPagamentoCartao(any(Pagamento.class), anyString(), any(CartaoCreditoRequestDTO.class), any()))
                    .thenReturn(asaasPaymentResponse);

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L, cartaoDTO, null);

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
                    () -> pagamentoService.criarPagamento(1L, null, null));
        }
    }

    @Test
    void deveProcessarReembolsoDinheiroComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pagamento.setMetodo(MetodoPagamento.CASH);
            pagamento.setStatus(StatusPagamento.PAID);
            pagamento.setValor(new BigDecimal("100.00"));

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pagamentoRepository.findByPedidoId(1L)).thenReturn(Optional.of(pagamento));
            doNothing().when(pagamentoValidator).validateReembolsoPossivel(any(Pagamento.class));
            when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                return p;
            });
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(modelMapper.map(any(Pagamento.class), eq(PagamentoResponseDTO.class))).thenReturn(pagamentoResponseDTO);

            PagamentoResponseDTO result = pagamentoService.processarReembolso(1L, "Teste de reembolso");

            assertNotNull(result);
            verify(pagamentoRepository, times(1)).save(argThat(p -> 
                p.getStatus() == StatusPagamento.REFUNDED &&
                p.getValorReembolsado().compareTo(new BigDecimal("100.00")) == 0 &&
                p.getDataReembolso() != null));
            verify(pedidoRepository, times(1)).save(argThat(p -> 
                p.getStatus() == StatusPedido.CANCELED));
        }
    }

    @Test
    void deveProcessarReembolsoEletronicoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pagamento.setMetodo(MetodoPagamento.PIX);
            pagamento.setStatus(StatusPagamento.PAID);
            pagamento.setValor(new BigDecimal("100.00"));
            pagamento.setAsaasPaymentId("pay_123456");

            AsaasRefundResponseDTO refundResponse = new AsaasRefundResponseDTO();
            refundResponse.setId("refund_123456");
            refundResponse.setValue("100.00");
            refundResponse.setStatus("REFUNDED");

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pagamentoRepository.findByPedidoId(1L)).thenReturn(Optional.of(pagamento));
            doNothing().when(pagamentoValidator).validateReembolsoPossivel(any(Pagamento.class));
            when(asaasService.estornarPagamento(anyString(), anyString())).thenReturn(refundResponse);
            when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
                Pagamento p = invocation.getArgument(0);
                return p;
            });
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);
            when(modelMapper.map(any(Pagamento.class), eq(PagamentoResponseDTO.class))).thenReturn(pagamentoResponseDTO);

            PagamentoResponseDTO result = pagamentoService.processarReembolso(1L, "Teste de reembolso");

            assertNotNull(result);
            verify(asaasService, times(1)).estornarPagamento("pay_123456", "Teste de reembolso");
            verify(pagamentoRepository, times(1)).save(argThat(p -> 
                p.getStatus() == StatusPagamento.REFUNDED &&
                p.getAsaasRefundId().equals("refund_123456") &&
                p.getDataReembolso() != null));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoNaoEncontradoParaReembolso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pagamentoRepository.findByPedidoId(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> pagamentoService.processarReembolso(1L, "Teste"));
        }
    }

    @Test
    void deveLancarExcecaoQuandoErroAoEstornarNoAsaas() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pagamento.setMetodo(MetodoPagamento.PIX);
            pagamento.setStatus(StatusPagamento.PAID);
            pagamento.setAsaasPaymentId("pay_123456");

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pagamentoRepository.findByPedidoId(1L)).thenReturn(Optional.of(pagamento));
            doNothing().when(pagamentoValidator).validateReembolsoPossivel(any(Pagamento.class));
            when(asaasService.estornarPagamento(anyString(), anyString()))
                    .thenThrow(new PaymentGatewayException("Erro ao estornar pagamento"));

            assertThrows(PaymentGatewayException.class, 
                    () -> pagamentoService.processarReembolso(1L, "Teste"));
        }
    }
}


