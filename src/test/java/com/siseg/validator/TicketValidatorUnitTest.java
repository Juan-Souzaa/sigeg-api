package com.siseg.validator;

import com.siseg.exception.AccessDeniedException;
import com.siseg.model.Role;
import com.siseg.model.Ticket;
import com.siseg.model.TicketComentario;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketValidatorUnitTest {
    
    @InjectMocks
    private TicketValidator ticketValidator;
    
    private User user;
    private User admin;
    private Ticket ticket;
    private TicketComentario comentario;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        Set<Role> userRoles = new HashSet<>();
        Role clienteRole = new Role();
        clienteRole.setRoleName(ERole.ROLE_CLIENTE);
        userRoles.add(clienteRole);
        user.setRoles(userRoles);
        
        admin = new User();
        admin.setId(2L);
        Set<Role> adminRoles = new HashSet<>();
        Role adminRole = new Role();
        adminRole.setRoleName(ERole.ROLE_ADMIN);
        adminRoles.add(adminRole);
        admin.setRoles(adminRoles);
        
        ticket = new Ticket();
        ticket.setId(1L);
        ticket.setCriadoPor(user);
        ticket.setStatus(StatusTicket.ABERTO);
        
        comentario = new TicketComentario();
        comentario.setInterno(false);
        comentario.setAutor(user);
    }
    
    @Test
    void deveValidarPermissaoCriarTicketParaCliente() {
        assertDoesNotThrow(() -> ticketValidator.validatePermissaoCriarTicket(user));
    }
    
    @Test
    void deveValidarPermissaoCriarTicketParaAdmin() {
        assertDoesNotThrow(() -> ticketValidator.validatePermissaoCriarTicket(admin));
    }
    
    @Test
    void deveLancarExcecaoParaUsuarioSemRoleValida() {
        User userSemRole = new User();
        userSemRole.setId(3L);
        userSemRole.setRoles(new HashSet<>());
        
        assertThrows(AccessDeniedException.class, () -> 
                ticketValidator.validatePermissaoCriarTicket(userSemRole));
    }
    
    @Test
    void deveValidarPermissaoResponderTicketParaAdmin() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(true);
            
            assertDoesNotThrow(() -> ticketValidator.validatePermissaoResponderTicket(ticket));
        }
    }
    
    @Test
    void deveLancarExcecaoParaNaoAdminResponderTicket() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            
            assertThrows(AccessDeniedException.class, () -> 
                    ticketValidator.validatePermissaoResponderTicket(ticket));
        }
    }
    
    @Test
    void deveValidarPermissaoVisualizarComentarioInternoParaAdmin() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::isAdmin).thenReturn(true);
            
            comentario.setInterno(true);
            assertDoesNotThrow(() -> 
                    ticketValidator.validatePermissaoVisualizarComentarioInterno(comentario, admin));
        }
    }
    
    @Test
    void deveLancarExcecaoParaNaoAdminVisualizarComentarioInterno() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::isAdmin).thenReturn(false);
            
            comentario.setInterno(true);
            assertThrows(AccessDeniedException.class, () -> 
                    ticketValidator.validatePermissaoVisualizarComentarioInterno(comentario, user));
        }
    }
    
    @Test
    void deveValidarTicketAberto() {
        ticket.setStatus(StatusTicket.ABERTO);
        assertDoesNotThrow(() -> ticketValidator.validateTicketAberto(ticket));
    }
    
    @Test
    void deveLancarExcecaoParaTicketResolvido() {
        ticket.setStatus(StatusTicket.RESOLVIDO);
        assertThrows(IllegalStateException.class, () -> ticketValidator.validateTicketAberto(ticket));
    }
    
    @Test
    void deveLancarExcecaoParaTicketFechado() {
        ticket.setStatus(StatusTicket.FECHADO);
        assertThrows(IllegalStateException.class, () -> ticketValidator.validateTicketAberto(ticket));
    }
    
    @Test
    void deveValidarPermissaoComentarTicketParaCriador() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::isAdmin).thenReturn(false);
            
            assertDoesNotThrow(() -> ticketValidator.validatePermissaoComentarTicket(ticket, user));
        }
    }
    
    @Test
    void deveValidarPermissaoComentarTicketParaAdmin() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::isAdmin).thenReturn(true);
            
            assertDoesNotThrow(() -> ticketValidator.validatePermissaoComentarTicket(ticket, admin));
        }
    }
    
    @Test
    void deveLancarExcecaoParaUsuarioSemPermissaoComentar() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::isAdmin).thenReturn(false);
            
            User outroUser = new User();
            outroUser.setId(3L);
            
            assertThrows(AccessDeniedException.class, () -> 
                    ticketValidator.validatePermissaoComentarTicket(ticket, outroUser));
        }
    }
    
    @Test
    void deveValidarPermissaoVisualizarTicketParaCriador() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::isAdmin).thenReturn(false);
            
            assertDoesNotThrow(() -> ticketValidator.validatePermissaoVisualizarTicket(ticket, user));
        }
    }
    
    @Test
    void deveValidarPermissaoVisualizarTicketParaAdmin() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::isAdmin).thenReturn(true);
            
            assertDoesNotThrow(() -> ticketValidator.validatePermissaoVisualizarTicket(ticket, admin));
        }
    }
    
    @Test
    void deveLancarExcecaoParaUsuarioSemPermissaoVisualizar() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::isAdmin).thenReturn(false);
            
            User outroUser = new User();
            outroUser.setId(3L);
            
            assertThrows(AccessDeniedException.class, () -> 
                    ticketValidator.validatePermissaoVisualizarTicket(ticket, outroUser));
        }
    }
}

