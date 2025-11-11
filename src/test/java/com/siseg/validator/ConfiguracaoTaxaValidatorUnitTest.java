package com.siseg.validator;

import com.siseg.model.enumerations.TipoTaxa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracaoTaxaValidatorUnitTest {

    @InjectMocks
    private ConfiguracaoTaxaValidator configuracaoTaxaValidator;

    @Test
    void deveValidarPercentualComValoresValidos() {
        assertDoesNotThrow(() -> configuracaoTaxaValidator.validatePercentual(new BigDecimal("0")));
        assertDoesNotThrow(() -> configuracaoTaxaValidator.validatePercentual(new BigDecimal("50")));
        assertDoesNotThrow(() -> configuracaoTaxaValidator.validatePercentual(new BigDecimal("100")));
    }

    @Test
    void deveLancarExcecaoQuandoPercentualNulo() {
        assertThrows(IllegalArgumentException.class, 
                () -> configuracaoTaxaValidator.validatePercentual(null));
    }

    @Test
    void deveLancarExcecaoQuandoPercentualNegativo() {
        assertThrows(IllegalArgumentException.class, 
                () -> configuracaoTaxaValidator.validatePercentual(new BigDecimal("-1")));
    }

    @Test
    void deveLancarExcecaoQuandoPercentualAcimaDe100() {
        assertThrows(IllegalArgumentException.class, 
                () -> configuracaoTaxaValidator.validatePercentual(new BigDecimal("101")));
    }

    @Test
    void deveValidarTipoTaxaComTipoValido() {
        assertDoesNotThrow(() -> configuracaoTaxaValidator.validateTipoTaxa(TipoTaxa.TAXA_RESTAURANTE));
        assertDoesNotThrow(() -> configuracaoTaxaValidator.validateTipoTaxa(TipoTaxa.TAXA_ENTREGADOR));
    }

    @Test
    void deveLancarExcecaoQuandoTipoTaxaNulo() {
        assertThrows(IllegalArgumentException.class, 
                () -> configuracaoTaxaValidator.validateTipoTaxa(null));
    }
}

