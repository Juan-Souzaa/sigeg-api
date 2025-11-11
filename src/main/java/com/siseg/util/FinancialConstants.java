package com.siseg.util;

import java.util.Arrays;
import java.util.List;

public final class FinancialConstants {
    private FinancialConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final List<String> PERIODOS_VALIDOS = Arrays.asList("HOJE", "SEMANA", "MES", "CUSTOMIZADO");
    public static final double PERCENTUAL_MINIMO = 0.0;
    public static final double PERCENTUAL_MAXIMO = 100.0;
    public static final double VALOR_DESCONTO_MAXIMO = 10000.0;
}

