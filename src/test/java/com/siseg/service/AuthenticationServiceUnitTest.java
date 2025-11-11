package com.siseg.service;

import com.siseg.model.User;
import com.siseg.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceUnitTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User user;
    private String token;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("test@teste.com");

        token = "jwt-token-12345";
    }

    @Test
    void deveAutenticarComSucesso() {
        when(authentication.getName()).thenReturn("test@teste.com");
        when(userRepository.findByUsername("test@teste.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(token);

        String result = authenticationService.authenticate(authentication);

        assertNotNull(result);
        assertEquals(token, result);
        verify(userRepository, times(1)).findByUsername("test@teste.com");
        verify(jwtService, times(1)).generateToken(user);
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        when(authentication.getName()).thenReturn("inexistente@teste.com");
        when(userRepository.findByUsername("inexistente@teste.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, 
                () -> authenticationService.authenticate(authentication));
        
        verify(userRepository, times(1)).findByUsername("inexistente@teste.com");
        verify(jwtService, never()).generateToken(any(User.class));
    }
}

