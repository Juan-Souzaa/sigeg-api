package com.siseg.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalculadoraFinanceira {
    private CalculadoraFinanceira() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static BigDecimal calcularTaxaPlataforma(BigDecimal valor, BigDecimal percentual) {
        if (valor == null || percentual == null) {
            return BigDecimal.ZERO;
        }
        if (valor.compareTo(BigDecimal.ZERO) <= 0 || percentual.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return valor.multiply(percentual)
                .divide(new BigDecimal("100"), SCALE, ROUNDING_MODE);
    }

    public static BigDecimal calcularValorLiquido(BigDecimal valor, BigDecimal taxa) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (taxa == null) {
            return valor.setScale(SCALE, ROUNDING_MODE);
        }
        BigDecimal valorLiquido = valor.subtract(taxa);
        return valorLiquido.compareTo(BigDecimal.ZERO) < 0 
                ? BigDecimal.ZERO 
                : valorLiquido.setScale(SCALE, ROUNDING_MODE);
    }
}

