package com.siseg.validator;

import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.AvaliacaoAlreadyExistsException;
import com.siseg.model.Avaliacao;
import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoValidatorUnitTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @InjectMocks
    private AvaliacaoValidator avaliacaoValidator;

    private User user;
    private Cliente cliente;
    private Pedido pedido;
    private Avaliacao avaliacao;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("cliente@teste.com");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setUser(user);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.DELIVERED);

        avaliacao = new Avaliacao();
        avaliacao.setId(1L);
        avaliacao.setCliente(cliente);
        avaliacao.setPedido(pedido);
    }

    @Test
    void deveValidarPermissaoAvaliacaoQuandoClienteDoPedido() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            doNothing().when(SecurityUtils.class);
            SecurityUtils.validatePedidoOwnership(pedido);

            assertDoesNotThrow(() -> avaliacaoValidator.validatePermissaoAvaliacao(pedido));
        }
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaAvaliacao() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            Pedido outroPedido = new Pedido();
            outroPedido.setCliente(new Cliente());

            doThrow(new AccessDeniedException("Você não tem permissão para acessar este pedido"))
                    .when(SecurityUtils.class);
            SecurityUtils.validatePedidoOwnership(outroPedido);

            assertThrows(AccessDeniedException.class, 
                    () -> avaliacaoValidator.validatePermissaoAvaliacao(outroPedido));
        }
    }

    @Test
    void deveValidarPedidoEntregueQuandoDelivered() {
        pedido.setStatus(StatusPedido.DELIVERED);

        assertDoesNotThrow(() -> avaliacaoValidator.validatePedidoEntregue(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoPedidoNaoEntregue() {
        pedido.setStatus(StatusPedido.CREATED);

        assertThrows(IllegalStateException.class, 
                () -> avaliacaoValidator.validatePedidoEntregue(pedido));
    }

    @Test
    void deveValidarAvaliacaoNaoExistenteQuandoSemAvaliacao() {
        when(avaliacaoRepository.existsByClienteIdAndPedidoId(1L, 1L)).thenReturn(false);

        assertDoesNotThrow(() -> avaliacaoValidator.validateAvaliacaoNaoExistente(1L, 1L));
    }

    @Test
    void deveLancarExcecaoQuandoAvaliacaoJaExiste() {
        when(avaliacaoRepository.existsByClienteIdAndPedidoId(1L, 1L)).thenReturn(true);

        assertThrows(AvaliacaoAlreadyExistsException.class, 
                () -> avaliacaoValidator.validateAvaliacaoNaoExistente(1L, 1L));
    }

    @Test
    void deveValidarOwnershipQuandoDonoDaAvaliacao() {
        assertDoesNotThrow(() -> avaliacaoValidator.validateOwnership(avaliacao, user));
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaOwnership() {
        User outroUser = new User();
        outroUser.setId(2L);

        assertThrows(AccessDeniedException.class, 
                () -> avaliacaoValidator.validateOwnership(avaliacao, outroUser));
    }
}

