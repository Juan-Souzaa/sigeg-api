package com.siseg.service;

import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.PedidoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceUnitTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PedidoValidator pedidoValidator;

    @Mock
    private PagamentoServiceClient pagamentoServiceClient;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Pedido pedido;
    private Cliente cliente;
    private User user;
    private PagamentoResponseDTO pagamentoResponseDTO;

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

        pagamentoResponseDTO = new PagamentoResponseDTO();
        pagamentoResponseDTO.setId(1L);
        pagamentoResponseDTO.setPedidoId(1L);
        pagamentoResponseDTO.setStatus(StatusPagamento.AUTHORIZED);
        pagamentoResponseDTO.setValor(new BigDecimal("100.00"));
    }

    @Test
    void deveCriarPagamentoPixComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doNothing().when(pedidoValidator).validateStatusParaConfirmacao(any(Pedido.class));
            when(pagamentoServiceClient.criarPagamento(any(Pedido.class), isNull(), isNull())).thenReturn(pagamentoResponseDTO);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L, null, null);

            assertNotNull(result);
            verify(pagamentoServiceClient, times(1)).criarPagamento(any(Pedido.class), isNull(), isNull());
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
                    .when(pedidoValidator).validateStatusParaConfirmacao(any(Pedido.class));

            assertThrows(RuntimeException.class, 
                    () -> pagamentoService.criarPagamento(1L, null, null));
        }
    }



    @Test
    void deveLancarExcecaoQuandoErroNoMicrosservico() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doNothing().when(pedidoValidator).validateStatusParaConfirmacao(any(Pedido.class));
            when(pagamentoServiceClient.criarPagamento(any(Pedido.class), isNull(), isNull()))
                    .thenThrow(new RuntimeException("Erro ao criar pagamento"));

            assertThrows(RuntimeException.class, 
                    () -> pagamentoService.criarPagamento(1L, null, null));
        }
    }



    @Test
    void deveBuscarPagamentoPorPedidoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pagamentoResponseDTO.setStatus(StatusPagamento.PAID);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pagamentoServiceClient.buscarPagamentoPorPedido(1L)).thenReturn(pagamentoResponseDTO);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

            PagamentoResponseDTO result = pagamentoService.buscarPagamentoPorPedido(1L);

            assertNotNull(result);
            verify(pagamentoServiceClient, times(1)).buscarPagamentoPorPedido(1L);
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }
    }

    @Test
    void deveProcessarPagamentoComCartaoCreditoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pedido.setMetodoPagamento(MetodoPagamento.CREDIT_CARD);
            pagamentoResponseDTO.setStatus(StatusPagamento.AUTHORIZED);

            CartaoCreditoRequestDTO cartaoDTO = new CartaoCreditoRequestDTO();
            cartaoDTO.setNumero("4111111111111111");
            cartaoDTO.setNomeTitular("Cliente Teste");
            cartaoDTO.setValidade("12/25");
            cartaoDTO.setCvv("123");

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            doNothing().when(pedidoValidator).validateStatusParaConfirmacao(any(Pedido.class));
            when(pagamentoServiceClient.criarPagamento(any(Pedido.class), any(CartaoCreditoRequestDTO.class), isNull()))
                    .thenReturn(pagamentoResponseDTO);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

            PagamentoResponseDTO result = pagamentoService.criarPagamento(1L, cartaoDTO, null);

            assertNotNull(result);
            verify(pagamentoServiceClient, times(1)).criarPagamento(any(Pedido.class), any(CartaoCreditoRequestDTO.class), isNull());
            verify(pedidoRepository, times(1)).save(any(Pedido.class));
        }
    }



    @Test
    void deveProcessarReembolsoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pagamentoResponseDTO.setStatus(StatusPagamento.PAID);
            pagamentoResponseDTO.setValorReembolsado(new BigDecimal("100.00"));

            PagamentoResponseDTO refundResponse = new PagamentoResponseDTO();
            refundResponse.setStatus(StatusPagamento.REFUNDED);
            refundResponse.setValorReembolsado(new BigDecimal("100.00"));

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pagamentoServiceClient.buscarPagamentoPorPedido(1L)).thenReturn(pagamentoResponseDTO);
            when(pagamentoServiceClient.processarReembolso(1L, "Teste de reembolso")).thenReturn(refundResponse);
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

            PagamentoResponseDTO result = pagamentoService.processarReembolso(1L, "Teste de reembolso");

            assertNotNull(result);
            verify(pagamentoServiceClient, times(1)).processarReembolso(1L, "Teste de reembolso");
            verify(pedidoRepository, times(1)).save(argThat(p -> 
                p.getStatus() == StatusPedido.CANCELED));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoJaReembolsado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            pagamentoResponseDTO.setStatus(StatusPagamento.REFUNDED);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pagamentoServiceClient.buscarPagamentoPorPedido(1L)).thenReturn(pagamentoResponseDTO);

            assertThrows(com.siseg.exception.PagamentoJaReembolsadoException.class, 
                    () -> pagamentoService.processarReembolso(1L, "Teste"));
        }
    }

}


