package com.siseg.service;

import com.siseg.model.Pedido;
import com.siseg.model.enumerations.TipoTaxa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxaCalculoServiceUnitTest {

    @Mock
    private ConfiguracaoTaxaService configuracaoTaxaService;

    @InjectMocks
    private TaxaCalculoService taxaCalculoService;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setSubtotal(new BigDecimal("100.00"));
        pedido.setTaxaEntrega(new BigDecimal("10.00"));
    }

    @Test
    void deveCalcularEAtualizarValoresFinanceirosComSucesso() {
        when(configuracaoTaxaService.obterPercentualTaxaAtiva(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(new BigDecimal("10.00"));
        when(configuracaoTaxaService.obterPercentualTaxaAtiva(TipoTaxa.TAXA_ENTREGADOR))
                .thenReturn(new BigDecimal("15.00"));

        taxaCalculoService.calcularEAtualizarValoresFinanceiros(pedido);

        assertNotNull(pedido.getTaxaPlataformaRestaurante());
        assertNotNull(pedido.getTaxaPlataformaEntregador());
        assertNotNull(pedido.getValorLiquidoRestaurante());
        assertNotNull(pedido.getValorLiquidoEntregador());
        assertEquals(new BigDecimal("10.00"), pedido.getTaxaPlataformaRestaurante());
        assertEquals(new BigDecimal("1.50"), pedido.getTaxaPlataformaEntregador());
        assertEquals(new BigDecimal("90.00"), pedido.getValorLiquidoRestaurante());
        assertEquals(new BigDecimal("8.50"), pedido.getValorLiquidoEntregador());
    }

    @Test
    void deveCalcularComValoresNulos() {
        pedido.setSubtotal(null);
        pedido.setTaxaEntrega(null);

        when(configuracaoTaxaService.obterPercentualTaxaAtiva(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(new BigDecimal("10.00"));
        when(configuracaoTaxaService.obterPercentualTaxaAtiva(TipoTaxa.TAXA_ENTREGADOR))
                .thenReturn(new BigDecimal("15.00"));

        taxaCalculoService.calcularEAtualizarValoresFinanceiros(pedido);

        assertEquals(0, pedido.getTaxaPlataformaRestaurante().compareTo(BigDecimal.ZERO));
        assertEquals(0, pedido.getTaxaPlataformaEntregador().compareTo(BigDecimal.ZERO));
        assertEquals(0, pedido.getValorLiquidoRestaurante().compareTo(BigDecimal.ZERO));
        assertEquals(0, pedido.getValorLiquidoEntregador().compareTo(BigDecimal.ZERO));
    }
}

