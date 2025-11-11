package com.siseg.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraFinanceiraUnitTest {

    @Test
    void deveCalcularTaxaPlataformaComSucesso() {
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal percentual = new BigDecimal("10.00");
        
        BigDecimal resultado = CalculadoraFinanceira.calcularTaxaPlataforma(valor, percentual);
        
        assertEquals(new BigDecimal("10.00"), resultado);
    }

    @Test
    void deveCalcularTaxaPlataformaComValorZero() {
        BigDecimal valor = BigDecimal.ZERO;
        BigDecimal percentual = new BigDecimal("10.00");
        
        BigDecimal resultado = CalculadoraFinanceira.calcularTaxaPlataforma(valor, percentual);
        
        assertEquals(BigDecimal.ZERO, resultado);
    }

    @Test
    void deveCalcularTaxaPlataformaComPercentualZero() {
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal percentual = BigDecimal.ZERO;
        
        BigDecimal resultado = CalculadoraFinanceira.calcularTaxaPlataforma(valor, percentual);
        
        assertEquals(BigDecimal.ZERO, resultado);
    }

    @Test
    void deveCalcularTaxaPlataformaComValoresNulos() {
        BigDecimal resultado1 = CalculadoraFinanceira.calcularTaxaPlataforma(null, new BigDecimal("10.00"));
        BigDecimal resultado2 = CalculadoraFinanceira.calcularTaxaPlataforma(new BigDecimal("100.00"), null);
        BigDecimal resultado3 = CalculadoraFinanceira.calcularTaxaPlataforma(null, null);
        
        assertEquals(BigDecimal.ZERO, resultado1);
        assertEquals(BigDecimal.ZERO, resultado2);
        assertEquals(BigDecimal.ZERO, resultado3);
    }

    @Test
    void deveCalcularTaxaPlataformaComArredondamento() {
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal percentual = new BigDecimal("33.33");
        
        BigDecimal resultado = CalculadoraFinanceira.calcularTaxaPlataforma(valor, percentual);
        
        assertEquals(new BigDecimal("33.33"), resultado);
    }

    @Test
    void deveCalcularValorLiquidoComSucesso() {
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal taxa = new BigDecimal("10.00");
        
        BigDecimal resultado = CalculadoraFinanceira.calcularValorLiquido(valor, taxa);
        
        assertEquals(new BigDecimal("90.00"), resultado);
    }

    @Test
    void deveCalcularValorLiquidoComTaxaNula() {
        BigDecimal valor = new BigDecimal("100.00");
        
        BigDecimal resultado = CalculadoraFinanceira.calcularValorLiquido(valor, null);
        
        assertEquals(new BigDecimal("100.00"), resultado);
    }

    @Test
    void deveCalcularValorLiquidoComValorNulo() {
        BigDecimal taxa = new BigDecimal("10.00");
        
        BigDecimal resultado = CalculadoraFinanceira.calcularValorLiquido(null, taxa);
        
        assertEquals(BigDecimal.ZERO, resultado);
    }

    @Test
    void deveCalcularValorLiquidoComTaxaMaiorQueValor() {
        BigDecimal valor = new BigDecimal("50.00");
        BigDecimal taxa = new BigDecimal("100.00");
        
        BigDecimal resultado = CalculadoraFinanceira.calcularValorLiquido(valor, taxa);
        
        assertEquals(BigDecimal.ZERO, resultado);
    }

    @Test
    void deveCalcularValorLiquidoComArredondamento() {
        BigDecimal valor = new BigDecimal("100.00");
        BigDecimal taxa = new BigDecimal("33.33");
        
        BigDecimal resultado = CalculadoraFinanceira.calcularValorLiquido(valor, taxa);
        
        assertEquals(new BigDecimal("66.67"), resultado);
    }
}

