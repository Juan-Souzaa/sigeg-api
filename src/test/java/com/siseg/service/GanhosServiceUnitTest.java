package com.siseg.service;

import com.siseg.dto.ganhos.GanhosEntregadorDTO;
import com.siseg.dto.ganhos.GanhosPorEntregaDTO;
import com.siseg.dto.ganhos.GanhosRestauranteDTO;
import com.siseg.dto.ganhos.RelatorioDistribuicaoDTO;
import com.siseg.mapper.GanhosMapper;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.Periodo;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GanhosServiceUnitTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private GanhosMapper ganhosMapper;

    @InjectMocks
    private GanhosService ganhosService;

    private Pedido pedido;
    private GanhosRestauranteDTO ganhosRestauranteDTO;
    private GanhosEntregadorDTO ganhosEntregadorDTO;

    @BeforeEach
    void setUp() {
        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setSubtotal(new BigDecimal("100.00"));
        pedido.setTaxaEntrega(new BigDecimal("10.00"));
        pedido.setTaxaPlataformaRestaurante(new BigDecimal("10.00"));
        pedido.setTaxaPlataformaEntregador(new BigDecimal("1.50"));
        pedido.setValorLiquidoRestaurante(new BigDecimal("90.00"));
        pedido.setValorLiquidoEntregador(new BigDecimal("8.50"));
        pedido.setTotal(new BigDecimal("110.00"));
        pedido.setCriadoEm(Instant.now());

        ganhosRestauranteDTO = new GanhosRestauranteDTO();
        ganhosRestauranteDTO.setValorBruto(new BigDecimal("100.00"));
        ganhosRestauranteDTO.setTaxaPlataforma(new BigDecimal("10.00"));
        ganhosRestauranteDTO.setValorLiquido(new BigDecimal("90.00"));

        ganhosEntregadorDTO = new GanhosEntregadorDTO();
        ganhosEntregadorDTO.setValorBruto(new BigDecimal("10.00"));
        ganhosEntregadorDTO.setTaxaPlataforma(new BigDecimal("1.50"));
        ganhosEntregadorDTO.setValorLiquido(new BigDecimal("8.50"));
    }

    @Test
    void deveCalcularGanhosRestauranteComSucesso() {
        when(pedidoRepository.findByStatusAndRestauranteIdAndCriadoEmBetween(
                any(StatusPedido.class), anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(pedido));
        when(ganhosMapper.toGanhosRestauranteDTO(any(), any(), any(), any(), anyLong(), anyString()))
                .thenReturn(ganhosRestauranteDTO);

        GanhosRestauranteDTO resultado = ganhosService.calcularGanhosRestaurante(1L, Periodo.MES);

        assertNotNull(resultado);
        verify(pedidoRepository).findByStatusAndRestauranteIdAndCriadoEmBetween(
                eq(StatusPedido.DELIVERED), eq(1L), any(Instant.class), any(Instant.class));
        verify(ganhosMapper).toGanhosRestauranteDTO(any(), any(), any(), any(), anyLong(), anyString());
    }

    @Test
    void deveCalcularGanhosEntregadorComSucesso() {
        when(pedidoRepository.findByStatusAndEntregadorIdAndCriadoEmBetween(
                any(StatusPedido.class), anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(pedido));
        when(ganhosMapper.toGanhosEntregadorDTO(any(), any(), any(), any(), anyLong(), anyString()))
                .thenReturn(ganhosEntregadorDTO);

        GanhosEntregadorDTO resultado = ganhosService.calcularGanhosEntregador(1L, Periodo.MES);

        assertNotNull(resultado);
        verify(pedidoRepository).findByStatusAndEntregadorIdAndCriadoEmBetween(
                eq(StatusPedido.DELIVERED), eq(1L), any(Instant.class), any(Instant.class));
        verify(ganhosMapper).toGanhosEntregadorDTO(any(), any(), any(), any(), anyLong(), anyString());
    }

    @Test
    void deveListarGanhosPorEntregaComSucesso() {
        when(pedidoRepository.findByStatusAndEntregadorIdAndCriadoEmBetween(
                any(StatusPedido.class), anyLong(), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(pedido));
        when(ganhosMapper.toGanhosPorEntregaDTOList(anyList()))
                .thenReturn(List.of(new GanhosPorEntregaDTO()));

        List<GanhosPorEntregaDTO> resultado = ganhosService.listarGanhosPorEntrega(1L, Periodo.MES);

        assertNotNull(resultado);
        verify(ganhosMapper).toGanhosPorEntregaDTOList(anyList());
    }

    @Test
    void deveGerarRelatorioDistribuicaoComSucesso() {
        when(pedidoRepository.findByStatus(StatusPedido.DELIVERED))
                .thenReturn(List.of(pedido));
        when(ganhosMapper.toRelatorioDistribuicaoDTO(any(), any(), any(), any(), anyString(), anyString()))
                .thenReturn(new RelatorioDistribuicaoDTO());

        RelatorioDistribuicaoDTO resultado = ganhosService.gerarRelatorioDistribuicao(Periodo.MES);

        assertNotNull(resultado);
        verify(ganhosMapper).toRelatorioDistribuicaoDTO(any(), any(), any(), any(), anyString(), anyString());
    }

    @Test
    void deveCalcularDistribuicaoPlataformaCorretamenteComCupom() {
        Pedido pedidoComCupom = new Pedido();
        pedidoComCupom.setId(2L);
        pedidoComCupom.setSubtotal(new BigDecimal("100.00"));
        pedidoComCupom.setTaxaEntrega(new BigDecimal("10.00"));
        pedidoComCupom.setTotal(new BigDecimal("90.00"));
        pedidoComCupom.setTaxaPlataformaRestaurante(new BigDecimal("10.00"));
        pedidoComCupom.setTaxaPlataformaEntregador(new BigDecimal("1.50"));
        pedidoComCupom.setValorLiquidoRestaurante(new BigDecimal("90.00"));
        pedidoComCupom.setValorLiquidoEntregador(new BigDecimal("8.50"));
        pedidoComCupom.setCriadoEm(Instant.now());

        when(pedidoRepository.findByStatus(StatusPedido.DELIVERED))
                .thenReturn(List.of(pedidoComCupom));

        RelatorioDistribuicaoDTO relatorio = new RelatorioDistribuicaoDTO();
        relatorio.setVolumeTotal(new BigDecimal("90.00"));
        relatorio.setDistribuicaoRestaurantes(new BigDecimal("90.00"));
        relatorio.setDistribuicaoEntregadores(new BigDecimal("8.50"));
        relatorio.setDistribuicaoPlataforma(new BigDecimal("11.50"));

        when(ganhosMapper.toRelatorioDistribuicaoDTO(
                eq(new BigDecimal("90.00")),
                eq(new BigDecimal("90.00")),
                eq(new BigDecimal("8.50")),
                eq(new BigDecimal("11.50")),
                anyString(),
                anyString()))
                .thenReturn(relatorio);

        RelatorioDistribuicaoDTO resultado = ganhosService.gerarRelatorioDistribuicao(Periodo.MES);

        assertNotNull(resultado);
        assertEquals(new BigDecimal("11.50"), resultado.getDistribuicaoPlataforma());
        verify(ganhosMapper).toRelatorioDistribuicaoDTO(
                eq(new BigDecimal("90.00")),
                eq(new BigDecimal("90.00")),
                eq(new BigDecimal("8.50")),
                eq(new BigDecimal("11.50")),
                anyString(),
                anyString());
    }
}

