package com.siseg.validator;

import com.siseg.model.Entregador;
import com.siseg.model.User;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntregadorValidatorUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntregadorRepository entregadorRepository;

    @InjectMocks
    private EntregadorValidator entregadorValidator;

    @Test
    void deveValidarEmailUnicoQuandoEmailUnico() {
        when(userRepository.findByUsername("novo@teste.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> entregadorValidator.validateEmailUnico("novo@teste.com"));
        verify(userRepository, times(1)).findByUsername("novo@teste.com");
    }

    @Test
    void deveLancarExcecaoQuandoEmailDuplicado() {
        User user = new User();
        user.setUsername("existente@teste.com");

        when(userRepository.findByUsername("existente@teste.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, 
                () -> entregadorValidator.validateEmailUnico("existente@teste.com"));
    }

    @Test
    void deveValidarCpfUnicoQuandoCpfUnico() {
        when(entregadorRepository.findByCpf("12345678900")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> entregadorValidator.validateCpfUnico("12345678900"));
        verify(entregadorRepository, times(1)).findByCpf("12345678900");
    }

    @Test
    void deveLancarExcecaoQuandoCpfDuplicado() {
        Entregador entregador = new Entregador();
        entregador.setCpf("12345678900");

        when(entregadorRepository.findByCpf("12345678900")).thenReturn(Optional.of(entregador));

        assertThrows(IllegalArgumentException.class, 
                () -> entregadorValidator.validateCpfUnico("12345678900"));
    }
}

