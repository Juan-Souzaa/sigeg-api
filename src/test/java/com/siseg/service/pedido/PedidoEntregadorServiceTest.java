package com.siseg.service.pedido;

import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.PedidoAlreadyProcessedException;
import com.siseg.model.*;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.mapper.PedidoMapper;
import com.siseg.repository.EntregadorRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoEntregadorServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EntregadorRepository entregadorRepository;

    @Mock
    private PedidoValidator pedidoValidator;

    @Mock
    private PedidoMapper pedidoMapper;

    @Mock
    private PedidoEnderecoService pedidoEnderecoService;

    @Mock
    private PedidoFinanceiroService pedidoFinanceiroService;

    @Mock
    private PedidoNotificacaoService pedidoNotificacaoService;

    @InjectMocks
    private PedidoEntregadorService pedidoEntregadorService;

    private User user;
    private Entregador entregador;
    private Pedido pedido;
    private Cliente cliente;
    private Restaurante restaurante;
    private Endereco enderecoEntrega;
    private Endereco enderecoRestaurante;
    private PedidoResponseDTO pedidoResponseDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("entregador@teste.com");

        entregador = new Entregador();
        entregador.setId(1L);
        entregador.setNome("Entregador Teste");
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);
        entregador.setStatus(StatusEntregador.APPROVED);
        entregador.setUser(user);
        entregador.setLatitude(new BigDecimal("-23.5505"));
        entregador.setLongitude(new BigDecimal("-46.6333"));

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");

        enderecoRestaurante = new Endereco();
        enderecoRestaurante.setId(1L);
        enderecoRestaurante.setLogradouro("Rua do Restaurante");
        enderecoRestaurante.setLatitude(new BigDecimal("-23.5505"));
        enderecoRestaurante.setLongitude(new BigDecimal("-46.6333"));
        enderecoRestaurante.setPrincipal(true);
        enderecoRestaurante.setTipo(TipoEndereco.OUTRO);

        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setNome("Restaurante Teste");
        restaurante.setEnderecos(List.of(enderecoRestaurante));
        enderecoRestaurante.setRestaurante(restaurante);

        enderecoEntrega = new Endereco();
        enderecoEntrega.setId(2L);
        enderecoEntrega.setLogradouro("Rua de Entrega");
        enderecoEntrega.setLatitude(new BigDecimal("-23.5631"));
        enderecoEntrega.setLongitude(new BigDecimal("-46.6542"));
        enderecoEntrega.setPrincipal(false);
        enderecoEntrega.setTipo(TipoEndereco.CASA);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setEnderecoEntrega(enderecoEntrega);
        pedido.setStatus(StatusPedido.PREPARING);
        pedido.setSubtotal(new BigDecimal("50.00"));
        pedido.setTotal(new BigDecimal("50.00"));

        pedidoResponseDTO = new PedidoResponseDTO();
        pedidoResponseDTO.setId(1L);
    }

    @Test
    void deveAceitarPedidoComSucesso() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoValidator.validateEntregadorAprovado(user)).thenReturn(entregador);
            doNothing().when(pedidoValidator).validatePedidoAceitavel(pedido);
            doNothing().when(pedidoEnderecoService).calcularEAtualizarTempoEstimadoEntrega(pedido, entregador);
            when(pedidoRepository.save(pedido)).thenReturn(pedido);
            when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);
            doNothing().when(pedidoNotificacaoService).enviarNotificacoesAceitePedido(pedido);

            PedidoResponseDTO result = pedidoEntregadorService.aceitarPedido(1L);

            assertNotNull(result);
            assertEquals(entregador, pedido.getEntregador());
            verify(pedidoRepository).save(pedido);
            verify(pedidoEnderecoService).calcularEAtualizarTempoEstimadoEntrega(pedido, entregador);
        }
    }

    @Test
    void deveLancarExcecaoQuandoEntregadorNaoAprovado() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            doThrow(new AccessDeniedException("Entregador não está aprovado"))
                    .when(pedidoValidator).validateEntregadorAprovado(user);

            assertThrows(AccessDeniedException.class, 
                    () -> pedidoEntregadorService.aceitarPedido(1L));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPedidoJaAceito() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            pedido.setEntregador(entregador);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoValidator.validateEntregadorAprovado(user)).thenReturn(entregador);
            doThrow(new PedidoAlreadyProcessedException("Pedido já foi aceito"))
                    .when(pedidoValidator).validatePedidoAceitavel(pedido);

            assertThrows(PedidoAlreadyProcessedException.class, 
                    () -> pedidoEntregadorService.aceitarPedido(1L));
        }
    }

    @Test
    void deveLancarExcecaoQuandoStatusInvalidoParaAceitar() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            pedido.setStatus(StatusPedido.CREATED);

            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(pedidoValidator.validateEntregadorAprovado(user)).thenReturn(entregador);
            doThrow(new IllegalStateException("Pedido deve estar PREPARING"))
                    .when(pedidoValidator).validatePedidoAceitavel(pedido);

            assertThrows(IllegalStateException.class, 
                    () -> pedidoEntregadorService.aceitarPedido(1L));
        }
    }

    @Test
    void deveRecusarPedidoComSucesso() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.of(entregador));
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

            pedidoEntregadorService.recusarPedido(1L);

            verify(pedidoRepository).findById(1L);
        }
    }

    @Test
    void deveLancarExcecaoQuandoRecusarPedidoDeOutroEntregador() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

            assertThrows(AccessDeniedException.class, 
                    () -> pedidoEntregadorService.recusarPedido(1L));
        }
    }

    @Test
    void deveMarcarSaiuEntregaComSucesso() {
        pedido.setEntregador(entregador);
        pedido.setStatus(StatusPedido.PREPARING);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        doNothing().when(pedidoValidator).validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode atualizar este status");
        doNothing().when(pedidoValidator).validateStatusParaSaiuEntrega(pedido);
        doNothing().when(pedidoEnderecoService).inicializarPosicaoEntregadorSeNecessario(pedido);
        doNothing().when(pedidoNotificacaoService).notificarClienteStatusPedido(pedido, "OUT_FOR_DELIVERY");
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        PedidoResponseDTO result = pedidoEntregadorService.marcarSaiuEntrega(1L);

        assertNotNull(result);
        assertEquals(StatusPedido.OUT_FOR_DELIVERY, pedido.getStatus());
        verify(pedidoEnderecoService).inicializarPosicaoEntregadorSeNecessario(pedido);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void deveLancarExcecaoQuandoMarcarSaiuSemEntregador() {
        pedido.setEntregador(null);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        doThrow(new IllegalStateException("Pedido não possui entregador"))
                .when(pedidoValidator).validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode atualizar este status");

        assertThrows(IllegalStateException.class, 
                () -> pedidoEntregadorService.marcarSaiuEntrega(1L));
    }

    @Test
    void deveMarcarComoEntregueComSucesso() {
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        pedido.setEntregador(entregador);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        doNothing().when(pedidoValidator).validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode marcar como entregue");
        doNothing().when(pedidoValidator).validateStatusParaEntrega(pedido);
        doNothing().when(pedidoFinanceiroService).calcularEAtualizarValoresPosEntrega(pedido);
        doNothing().when(pedidoNotificacaoService).enviarNotificacoesEntregaPedido(pedido);
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

        PedidoResponseDTO result = pedidoEntregadorService.marcarComoEntregue(1L);

        assertNotNull(result);
        assertEquals(StatusPedido.DELIVERED, pedido.getStatus());
        verify(pedidoFinanceiroService).calcularEAtualizarValoresPosEntrega(pedido);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void deveLancarExcecaoQuandoMarcarEntregueDeOutroEntregador() {
        Entregador outroEntregador = new Entregador();
        outroEntregador.setId(2L);
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        pedido.setEntregador(outroEntregador);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        doThrow(new AccessDeniedException("Apenas o entregador associado pode marcar como entregue"))
                .when(pedidoValidator).validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode marcar como entregue");

        assertThrows(AccessDeniedException.class, 
                () -> pedidoEntregadorService.marcarComoEntregue(1L));
    }

    @Test
    void deveListarPedidosDisponiveis() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Pedido> page = new PageImpl<>(List.of(pedido));

            when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.of(entregador));
            when(pedidoRepository.findByStatusAndEntregadorIsNull(StatusPedido.PREPARING, pageable))
                    .thenReturn(page);
            when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoEntregadorService.listarPedidosDisponiveis(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByStatusAndEntregadorIsNull(StatusPedido.PREPARING, pageable);
        }
    }

    @Test
    void deveListarEntregasAtivas() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            pedido.setEntregador(entregador);
            pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
            Page<Pedido> page = new PageImpl<>(List.of(pedido));

            when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.of(entregador));
            when(pedidoRepository.findByEntregadorIdAndStatusNotIn(
                    eq(entregador.getId()), 
                    eq(List.of(StatusPedido.DELIVERED, StatusPedido.CANCELED)), 
                    eq(pageable)))
                    .thenReturn(page);
            when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoEntregadorService.listarEntregasAtivas(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByEntregadorIdAndStatusNotIn(
                    eq(entregador.getId()), 
                    eq(List.of(StatusPedido.DELIVERED, StatusPedido.CANCELED)), 
                    eq(pageable));
        }
    }

    @Test
    void deveListarHistoricoEntregas() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUser).thenReturn(user);

            Pageable pageable = PageRequest.of(0, 10);
            pedido.setEntregador(entregador);
            pedido.setStatus(StatusPedido.DELIVERED);
            Page<Pedido> page = new PageImpl<>(List.of(pedido));

            when(entregadorRepository.findByUserId(user.getId())).thenReturn(Optional.of(entregador));
            when(pedidoRepository.findByEntregadorIdAndStatus(
                    entregador.getId(), StatusPedido.DELIVERED, pageable))
                    .thenReturn(page);
            when(pedidoMapper.toResponseDTO(pedido)).thenReturn(pedidoResponseDTO);

            Page<PedidoResponseDTO> result = pedidoEntregadorService.listarHistoricoEntregas(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(pedidoRepository).findByEntregadorIdAndStatus(
                    entregador.getId(), StatusPedido.DELIVERED, pageable);
        }
    }
}

