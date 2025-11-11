package com.siseg.validator;

import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cupom;
import com.siseg.model.enumerations.TipoDesconto;
import com.siseg.repository.CupomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CupomValidatorUnitTest {

    @Mock
    private CupomRepository cupomRepository;

    @InjectMocks
    private CupomValidator cupomValidator;

    private Cupom cupom;

    @BeforeEach
    void setUp() {
        cupom = new Cupom();
        cupom.setId(1L);
        cupom.setCodigo("DESCONTO10");
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValorDesconto(new BigDecimal("10.00"));
        cupom.setValorMinimo(new BigDecimal("50.00"));
        cupom.setDataInicio(LocalDate.now().minusDays(5));
        cupom.setDataFim(LocalDate.now().plusDays(25));
        cupom.setUsosMaximos(100);
        cupom.setUsosAtuais(50);
        cupom.setAtivo(true);
    }

    @Test
    void deveValidarCodigoUnicoComSucesso() {
        when(cupomRepository.findByCodigo("NOVO_CODIGO")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> cupomValidator.validateCodigoUnico("NOVO_CODIGO"));
    }

    @Test
    void deveLancarExcecaoQuandoCodigoJaExiste() {
        when(cupomRepository.findByCodigo("DESCONTO10")).thenReturn(Optional.of(cupom));

        assertThrows(IllegalArgumentException.class, () -> 
                cupomValidator.validateCodigoUnico("DESCONTO10"));
    }

    @Test
    void deveValidarCupomValidoComSucesso() {
        assertDoesNotThrow(() -> cupomValidator.validateCupomValido(cupom));
    }

    @Test
    void deveLancarExcecaoQuandoCupomNulo() {
        assertThrows(ResourceNotFoundException.class, () -> 
                cupomValidator.validateCupomValido(null));
    }

    @Test
    void deveLancarExcecaoQuandoCupomDesativado() {
        cupom.setAtivo(false);

        assertThrows(IllegalArgumentException.class, () -> 
                cupomValidator.validateCupomValido(cupom));
    }

    @Test
    void deveValidarCupomAplicavelComSucesso() {
        BigDecimal valorMinimo = new BigDecimal("60.00");

        assertDoesNotThrow(() -> cupomValidator.validateCupomAplicavel(cupom, valorMinimo));
    }

    @Test
    void deveLancarExcecaoQuandoCupomAplicavelExpirado() {
        cupom.setDataInicio(LocalDate.now().plusDays(10));
        cupom.setDataFim(LocalDate.now().plusDays(20));
        BigDecimal valorMinimo = new BigDecimal("60.00");

        assertThrows(IllegalArgumentException.class, () -> 
                cupomValidator.validateCupomAplicavel(cupom, valorMinimo));
    }

    @Test
    void deveLancarExcecaoQuandoUsosMaximosAtingidos() {
        cupom.setUsosAtuais(100);
        BigDecimal valorMinimo = new BigDecimal("60.00");

        assertThrows(IllegalArgumentException.class, () -> 
                cupomValidator.validateCupomAplicavel(cupom, valorMinimo));
    }

    @Test
    void deveLancarExcecaoQuandoValorMinimoNaoAtendido() {
        BigDecimal valorMinimo = new BigDecimal("30.00");

        assertThrows(IllegalArgumentException.class, () -> 
                cupomValidator.validateCupomAplicavel(cupom, valorMinimo));
    }

    @Test
    void deveValidarCupomNaoExpiradoComSucesso() {
        assertDoesNotThrow(() -> cupomValidator.validateCupomNaoExpirado(cupom));
    }

    @Test
    void deveLancarExcecaoQuandoCupomExpirado() {
        cupom.setDataFim(LocalDate.now().minusDays(1));

        assertThrows(IllegalArgumentException.class, () -> 
                cupomValidator.validateCupomNaoExpirado(cupom));
    }

    @Test
    void deveLancarExcecaoQuandoCupomAindaNaoIniciado() {
        cupom.setDataInicio(LocalDate.now().plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> 
                cupomValidator.validateCupomNaoExpirado(cupom));
    }
}

