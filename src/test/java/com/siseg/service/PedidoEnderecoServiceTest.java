package com.siseg.service;

import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.repository.EntregadorRepository;
import com.siseg.service.pedido.PedidoEnderecoService;
import com.siseg.util.TempoEstimadoCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoEnderecoServiceTest {

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private TempoEstimadoCalculator tempoEstimadoCalculator;

    @Mock
    private EntregadorRepository entregadorRepository;

    @InjectMocks
    private PedidoEnderecoService pedidoEnderecoService;

    private Cliente cliente;
    private Restaurante restaurante;
    private Endereco enderecoPrincipal;
    private Endereco enderecoEspecifico;
    private Pedido pedido;
    private PedidoRequestDTO pedidoRequestDTO;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);

        restaurante = new Restaurante();
        restaurante.setId(2L);

        enderecoPrincipal = new Endereco();
        enderecoPrincipal.setId(10L);
        enderecoPrincipal.setCliente(cliente);
        enderecoPrincipal.setTipo(TipoEndereco.CASA);
        enderecoPrincipal.setLatitude(new BigDecimal("-23.55"));
        enderecoPrincipal.setLongitude(new BigDecimal("-46.63"));

        enderecoEspecifico = new Endereco();
        enderecoEspecifico.setId(20L);
        enderecoEspecifico.setCliente(cliente);
        enderecoEspecifico.setTipo(TipoEndereco.OUTRO);
        enderecoEspecifico.setLatitude(new BigDecimal("-23.54"));
        enderecoEspecifico.setLongitude(new BigDecimal("-46.62"));

        Endereco enderecoRestaurante = new Endereco();
        enderecoRestaurante.setId(30L);
        enderecoRestaurante.setRestaurante(restaurante);
        enderecoRestaurante.setLatitude(new BigDecimal("-23.53"));
        enderecoRestaurante.setLongitude(new BigDecimal("-46.60"));
        enderecoRestaurante.setPrincipal(true);
        restaurante.setEnderecos(List.of(enderecoRestaurante));

        pedido = new Pedido();
        pedido.setRestaurante(restaurante);

        pedidoRequestDTO = new PedidoRequestDTO();
        pedidoRequestDTO.setMetodoPagamento(MetodoPagamento.CASH);
        pedidoRequestDTO.setRestauranteId(restaurante.getId());
    }

    @Test
    void deveProcessarEnderecoPrincipalComGeocodificacao() {
        enderecoPrincipal.setLatitude(null);
        enderecoPrincipal.setLongitude(null);

        when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())).thenReturn(Optional.of(enderecoPrincipal));
        when(enderecoService.geocodificarESalvar(enderecoPrincipal)).thenAnswer(invocation -> {
            Endereco endereco = invocation.getArgument(0);
            endereco.setLatitude(new BigDecimal("-23.55"));
            endereco.setLongitude(new BigDecimal("-46.63"));
            return endereco;
        });

        pedidoEnderecoService.processarEnderecoEntrega(pedido, cliente, pedidoRequestDTO);

        assertEquals(enderecoPrincipal, pedido.getEnderecoEntrega());
        verify(enderecoService).geocodificarESalvar(enderecoPrincipal);
    }

    @Test
    void deveProcessarEnderecoPorIdQuandoInformado() {
        pedidoRequestDTO.setEnderecoId(enderecoEspecifico.getId());
        when(enderecoService.buscarEnderecoPorIdECliente(enderecoEspecifico.getId(), cliente.getId()))
            .thenReturn(Optional.of(enderecoEspecifico));

        pedidoEnderecoService.processarEnderecoEntrega(pedido, cliente, pedidoRequestDTO);

        assertEquals(enderecoEspecifico, pedido.getEnderecoEntrega());
        verify(enderecoService, never()).buscarEnderecoPrincipalCliente(cliente.getId());
    }

    @Test
    void deveCalcularTempoEstimadoComCoordenadasValidas() {
        pedido.setEnderecoEntrega(enderecoPrincipal);
        Entregador entregador = new Entregador();
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);

        ResultadoCalculo resultado = new ResultadoCalculo(new BigDecimal("2.5"), 15, true);
        when(tempoEstimadoCalculator.calculateDistanceAndTime(
            any(), any(), any(), any(), eq(TipoVeiculo.MOTO)
        )).thenReturn(resultado);

        pedidoEnderecoService.calcularEAtualizarTempoEstimadoEntrega(pedido, entregador);

        assertNotNull(pedido.getTempoEstimadoEntrega());
        verify(tempoEstimadoCalculator).calculateDistanceAndTime(
            eq(restaurante.getEnderecos().get(0).getLatitude()),
            eq(restaurante.getEnderecos().get(0).getLongitude()),
            eq(enderecoPrincipal.getLatitude()),
            eq(enderecoPrincipal.getLongitude()),
            eq(TipoVeiculo.MOTO)
        );
    }

    @Test
    void deveInicializarPosicaoDoEntregadorQuandoNecessario() {
        Entregador entregador = new Entregador();
        entregador.setTipoVeiculo(TipoVeiculo.CARRO);
        pedido.setEntregador(entregador);

        pedidoEnderecoService.inicializarPosicaoEntregadorSeNecessario(pedido);

        verify(entregadorRepository).save(entregador);
        assertEquals(restaurante.getEnderecos().get(0).getLatitude(), entregador.getLatitude());
        assertEquals(restaurante.getEnderecos().get(0).getLongitude(), entregador.getLongitude());
    }

    @Test
    void naoDeveInicializarPosicaoQuandoRestauranteSemCoordenadas() {
        Entregador entregador = new Entregador();
        pedido.setEntregador(entregador);
        Endereco enderecoSemCoordenadas = new Endereco();
        enderecoSemCoordenadas.setPrincipal(true);
        restaurante.setEnderecos(List.of(enderecoSemCoordenadas));

        pedidoEnderecoService.inicializarPosicaoEntregadorSeNecessario(pedido);

        verify(entregadorRepository, never()).save(any());
    }

    @Test
    void naoDeveInicializarPosicaoQuandoPedidoSemEntregador() {
        pedidoEnderecoService.inicializarPosicaoEntregadorSeNecessario(pedido);

        verify(entregadorRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoEnderecoNaoEncontrado() {
        pedidoRequestDTO.setEnderecoId(999L);

        when(enderecoService.buscarEnderecoPorIdECliente(999L, cliente.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> pedidoEnderecoService.processarEnderecoEntrega(pedido, cliente, pedidoRequestDTO));
    }

    @Test
    void deveLancarExcecaoQuandoClienteSemEndereco() {
        when(enderecoService.buscarEnderecoPrincipalCliente(cliente.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
            () -> pedidoEnderecoService.processarEnderecoEntrega(pedido, cliente, pedidoRequestDTO));
    }
}


