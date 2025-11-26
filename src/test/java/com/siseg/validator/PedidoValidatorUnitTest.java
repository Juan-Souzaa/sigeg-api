package com.siseg.validator;

import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.PedidoAlreadyProcessedException;
import com.siseg.exception.PratoNotAvailableException;
import com.siseg.model.Cliente;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Prato;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.DisponibilidadeEntregador;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.EntregadorRepository;
import com.siseg.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoValidatorUnitTest {

    @Mock
    private EntregadorRepository entregadorRepository;

    @InjectMocks
    private PedidoValidator pedidoValidator;

    private User user;
    private Entregador entregador;
    private Pedido pedido;
    private Cliente cliente;
    private Prato prato;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("entregador@teste.com");

        entregador = new Entregador();
        entregador.setId(1L);
        entregador.setNome("Entregador Teste");
        entregador.setStatus(StatusEntregador.APPROVED);
        entregador.setDisponibilidade(DisponibilidadeEntregador.AVAILABLE);
        entregador.setUser(user);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setUser(user);

        Restaurante restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setNome("Restaurante Teste");

        prato = new Prato();
        prato.setId(1L);
        prato.setNome("Prato Teste");
        prato.setDisponivel(true);
        prato.setRestaurante(restaurante);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setStatus(StatusPedido.PREPARING);
    }

    @Test
    void deveValidarEntregadorAprovadoComSucesso() {
        when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.of(entregador));

        Entregador result = pedidoValidator.validateEntregadorAprovado(user);

        assertNotNull(result);
        assertEquals(entregador, result);
        verify(entregadorRepository, times(1)).findByUserId(user.getId());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEhEntregador() {
        when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, 
                () -> pedidoValidator.validateEntregadorAprovado(user));
    }

    @Test
    void deveLancarExcecaoQuandoEntregadorNaoAprovado() {
        entregador.setStatus(StatusEntregador.PENDING_APPROVAL);

        when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.of(entregador));

        assertThrows(AccessDeniedException.class, 
                () -> pedidoValidator.validateEntregadorAprovado(user));
    }

    @Test
    void deveValidarPedidoAceitavelComSucesso() {
        pedido.setStatus(StatusPedido.PREPARING);
        pedido.setEntregador(null);

        assertDoesNotThrow(() -> pedidoValidator.validatePedidoAceitavel(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoStatusInvalidoParaAceitar() {
        pedido.setStatus(StatusPedido.CREATED);

        assertThrows(PedidoAlreadyProcessedException.class, 
                () -> pedidoValidator.validatePedidoAceitavel(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoPedidoJaAceito() {
        pedido.setStatus(StatusPedido.PREPARING);
        pedido.setEntregador(entregador);

        assertThrows(PedidoAlreadyProcessedException.class, 
                () -> pedidoValidator.validatePedidoAceitavel(pedido));
    }

    @Test
    void deveValidarStatusPreparoComSucesso() {
        pedido.setStatus(StatusPedido.CONFIRMED);

        assertDoesNotThrow(() -> pedidoValidator.validateStatusPreparo(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoStatusInvalidoParaPreparo() {
        pedido.setStatus(StatusPedido.CREATED);

        assertThrows(PedidoAlreadyProcessedException.class, 
                () -> pedidoValidator.validateStatusPreparo(pedido));
    }

    @Test
    void devePermitirAdminBypassParaEntregadorDoPedido() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            assertDoesNotThrow(() -> pedidoValidator.validateEntregadorDoPedido(pedido, "Mensagem de erro"));
        }
    }

    @Test
    void deveValidarEntregadorCorretoDoPedido() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            pedido.setEntregador(entregador);

            doNothing().when(SecurityUtils.class);
            SecurityUtils.validateEntregadorOwnership(entregador);

            assertDoesNotThrow(() -> pedidoValidator.validateEntregadorDoPedido(pedido, "Mensagem de erro"));
        }
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaEntregadorDoPedido() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            pedido.setEntregador(null);

            assertThrows(AccessDeniedException.class, 
                    () -> pedidoValidator.validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode atualizar este status"));
        }
    }

    @Test
    void deveValidarStatusParaConfirmacaoComSucesso() {
        pedido.setStatus(StatusPedido.CREATED);

        assertDoesNotThrow(() -> pedidoValidator.validateStatusParaConfirmacao(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoStatusInvalidoParaConfirmacao() {
        pedido.setStatus(StatusPedido.CONFIRMED);

        assertThrows(PedidoAlreadyProcessedException.class, 
                () -> pedidoValidator.validateStatusParaConfirmacao(pedido));
    }

    @Test
    void deveValidarStatusParaSaiuEntregaComSucesso() {
        pedido.setStatus(StatusPedido.PREPARING);

        assertDoesNotThrow(() -> pedidoValidator.validateStatusParaSaiuEntrega(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoStatusInvalidoParaSaiuEntrega() {
        pedido.setStatus(StatusPedido.CONFIRMED);

        assertThrows(PedidoAlreadyProcessedException.class, 
                () -> pedidoValidator.validateStatusParaSaiuEntrega(pedido));
    }

    @Test
    void deveValidarStatusParaEntregaComSucesso() {
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);

        assertDoesNotThrow(() -> pedidoValidator.validateStatusParaEntrega(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoStatusInvalidoParaEntrega() {
        pedido.setStatus(StatusPedido.PREPARING);

        assertThrows(PedidoAlreadyProcessedException.class, 
                () -> pedidoValidator.validateStatusParaEntrega(pedido));
    }

    @Test
    void devePermitirAdminParaPermissaoCliente() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            Cliente outroCliente = new Cliente();
            outroCliente.setUser(new User());

            assertDoesNotThrow(() -> pedidoValidator.validatePermissaoCliente(outroCliente, user));
        }
    }

    @Test
    void deveValidarClienteCorreto() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            assertDoesNotThrow(() -> pedidoValidator.validatePermissaoCliente(cliente, user));
        }
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaCliente() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            Cliente outroCliente = new Cliente();
            User outroUser = new User();
            outroUser.setId(2L);
            outroCliente.setUser(outroUser);

            assertThrows(AccessDeniedException.class, 
                    () -> pedidoValidator.validatePermissaoCliente(outroCliente, user));
        }
    }

    @Test
    void deveValidarPratoDisponivelComSucesso() {
        prato.setDisponivel(true);

        Prato result = pedidoValidator.validatePratoDisponivel(prato);

        assertNotNull(result);
        assertEquals(prato, result);
    }

    @Test
    void deveLancarExcecaoQuandoPratoIndisponivel() {
        prato.setDisponivel(false);

        assertThrows(PratoNotAvailableException.class, 
                () -> pedidoValidator.validatePratoDisponivel(prato));
    }
}

