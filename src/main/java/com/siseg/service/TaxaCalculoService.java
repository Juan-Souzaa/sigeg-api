package com.siseg.service;

import com.siseg.model.Pedido;
import com.siseg.model.enumerations.TipoTaxa;
import com.siseg.util.CalculadoraFinanceira;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TaxaCalculoService {

    private final ConfiguracaoTaxaService configuracaoTaxaService;

    public TaxaCalculoService(ConfiguracaoTaxaService configuracaoTaxaService) {
        this.configuracaoTaxaService = configuracaoTaxaService;
    }

    public void calcularEAtualizarValoresFinanceiros(Pedido pedido) {
        BigDecimal subtotalRestaurante = obterSubtotalRestaurante(pedido);
        BigDecimal taxaEntrega = obterTaxaEntrega(pedido);

        BigDecimal percentualTaxaRestaurante = obterPercentualTaxa(TipoTaxa.TAXA_RESTAURANTE);
        BigDecimal percentualTaxaEntregador = obterPercentualTaxa(TipoTaxa.TAXA_ENTREGADOR);

        BigDecimal taxaPlataformaRestaurante = calcularTaxaPlataforma(subtotalRestaurante, percentualTaxaRestaurante);
        BigDecimal taxaPlataformaEntregador = calcularTaxaPlataforma(taxaEntrega, percentualTaxaEntregador);

        BigDecimal valorLiquidoRestaurante = calcularValorLiquido(subtotalRestaurante, taxaPlataformaRestaurante);
        BigDecimal valorLiquidoEntregador = calcularValorLiquido(taxaEntrega, taxaPlataformaEntregador);

        atualizarPedidoComValoresFinanceiros(pedido, taxaPlataformaRestaurante, taxaPlataformaEntregador,
                valorLiquidoRestaurante, valorLiquidoEntregador);
    }

    private BigDecimal obterSubtotalRestaurante(Pedido pedido) {
        return pedido.getSubtotal() != null ? pedido.getSubtotal() : BigDecimal.ZERO;
    }

    private BigDecimal obterTaxaEntrega(Pedido pedido) {
        return pedido.getTaxaEntrega() != null ? pedido.getTaxaEntrega() : BigDecimal.ZERO;
    }

    private BigDecimal obterPercentualTaxa(TipoTaxa tipoTaxa) {
        return configuracaoTaxaService.obterPercentualTaxaAtiva(tipoTaxa);
    }

    private BigDecimal calcularTaxaPlataforma(BigDecimal valor, BigDecimal percentual) {
        return CalculadoraFinanceira.calcularTaxaPlataforma(valor, percentual);
    }

    private BigDecimal calcularValorLiquido(BigDecimal valor, BigDecimal taxa) {
        return CalculadoraFinanceira.calcularValorLiquido(valor, taxa);
    }

    private void atualizarPedidoComValoresFinanceiros(Pedido pedido, BigDecimal taxaPlataformaRestaurante,
                                                       BigDecimal taxaPlataformaEntregador,
                                                       BigDecimal valorLiquidoRestaurante,
                                                       BigDecimal valorLiquidoEntregador) {
        pedido.setTaxaPlataformaRestaurante(taxaPlataformaRestaurante);
        pedido.setTaxaPlataformaEntregador(taxaPlataformaEntregador);
        pedido.setValorLiquidoRestaurante(valorLiquidoRestaurante);
        pedido.setValorLiquidoEntregador(valorLiquidoEntregador);
    }
}

