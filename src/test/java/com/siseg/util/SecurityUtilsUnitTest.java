package com.siseg.util;

import com.siseg.exception.AccessDeniedException;
import com.siseg.model.*;
import com.siseg.model.User;
import com.siseg.model.UserAuthenticated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityUtilsUnitTest {

    private User user;
    private Cliente cliente;
    private Restaurante restaurante;
    private Entregador entregador;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("test@teste.com");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setUser(user);

        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setUser(user);

        entregador = new Entregador();
        entregador.setId(1L);
        entregador.setUser(user);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setEntregador(entregador);
    }

    @Test
    void deveObterUsuarioAutenticado() {
        Authentication authentication = mock(Authentication.class);
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);
        
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userAuthenticated);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        User result = SecurityUtils.getCurrentUser();

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoAutenticado() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> SecurityUtils.getCurrentUser());
    }

    @Test
    void deveVerificarSeUsuarioEhAdmin() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Collection<GrantedAuthority> authorities = List.of(adminAuthority);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertTrue(SecurityUtils.isAdmin());
    }

    @Test
    void deveRetornarFalseQuandoUsuarioNaoEhAdmin() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority clienteAuthority = new SimpleGrantedAuthority("ROLE_CLIENTE");
        Collection<GrantedAuthority> authorities = List.of(clienteAuthority);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertFalse(SecurityUtils.isAdmin());
    }

    @Test
    void devePermitirAdminParaPedidoOwnership() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_ADMIN");
        Collection<GrantedAuthority> authorities = List.of(adminAuthority);
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(authentication.getPrincipal()).thenReturn(userAuthenticated);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertDoesNotThrow(() -> SecurityUtils.validatePedidoOwnership(pedido));
    }

    @Test
    void devePermitirClienteOwnerParaPedidoOwnership() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority clienteAuthority = new SimpleGrantedAuthority("ROLE_CLIENTE");
        Collection<GrantedAuthority> authorities = List.of(clienteAuthority);
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(authentication.getPrincipal()).thenReturn(userAuthenticated);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertDoesNotThrow(() -> SecurityUtils.validatePedidoOwnership(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaPedido() {
        User outroUser = new User();
        outroUser.setId(2L);

        Authentication authentication = mock(Authentication.class);
        GrantedAuthority clienteAuthority = new SimpleGrantedAuthority("ROLE_CLIENTE");
        Collection<GrantedAuthority> authorities = List.of(clienteAuthority);
        UserAuthenticated userAuthenticated = new UserAuthenticated(outroUser);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(authentication.getPrincipal()).thenReturn(userAuthenticated);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Pedido outroPedido = new Pedido();
        outroPedido.setCliente(cliente);

        assertThrows(AccessDeniedException.class,
                () -> SecurityUtils.validatePedidoOwnership(outroPedido));
    }

    @Test
    void devePermitirRestauranteOwnerParaRestauranteOwnership() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority restauranteAuthority = new SimpleGrantedAuthority("ROLE_RESTAURANTE");
        Collection<GrantedAuthority> authorities = List.of(restauranteAuthority);
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(authentication.getPrincipal()).thenReturn(userAuthenticated);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertDoesNotThrow(() -> SecurityUtils.validateRestauranteOwnership(restaurante));
    }

    @Test
    void devePermitirEntregadorOwnerParaEntregadorOwnership() {
        Authentication authentication = mock(Authentication.class);
        GrantedAuthority entregadorAuthority = new SimpleGrantedAuthority("ROLE_ENTREGADOR");
        Collection<GrantedAuthority> authorities = List.of(entregadorAuthority);
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getAuthorities()).thenAnswer(invocation -> authorities);
        when(authentication.getPrincipal()).thenReturn(userAuthenticated);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        assertDoesNotThrow(() -> SecurityUtils.validateEntregadorOwnership(entregador));
    }
}

