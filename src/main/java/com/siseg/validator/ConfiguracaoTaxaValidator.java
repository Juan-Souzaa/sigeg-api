package com.siseg.validator;

import com.siseg.model.enumerations.TipoTaxa;
import com.siseg.util.FinancialConstants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ConfiguracaoTaxaValidator {

    public void validatePercentual(BigDecimal percentual) {
        if (percentual == null) {
            throw new IllegalArgumentException("Percentual não pode ser nulo");
        }
        if (percentual.compareTo(BigDecimal.valueOf(FinancialConstants.PERCENTUAL_MINIMO)) < 0) {
            throw new IllegalArgumentException("Percentual deve ser maior ou igual a " + FinancialConstants.PERCENTUAL_MINIMO);
        }
        if (percentual.compareTo(BigDecimal.valueOf(FinancialConstants.PERCENTUAL_MAXIMO)) > 0) {
            throw new IllegalArgumentException("Percentual deve ser menor ou igual a " + FinancialConstants.PERCENTUAL_MAXIMO);
        }
    }

    public void validateTipoTaxa(TipoTaxa tipoTaxa) {
        if (tipoTaxa == null) {
            throw new IllegalArgumentException("Tipo de taxa não pode ser nulo");
        }
    }
}

