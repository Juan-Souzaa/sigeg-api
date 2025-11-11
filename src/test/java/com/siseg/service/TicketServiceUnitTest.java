package com.siseg.service;

import com.siseg.dto.ticket.TicketComentarioRequestDTO;
import com.siseg.dto.ticket.TicketComentarioResponseDTO;
import com.siseg.dto.ticket.TicketRequestDTO;
import com.siseg.dto.ticket.TicketResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.mapper.TicketMapper;
import com.siseg.model.Role;
import com.siseg.model.Ticket;
import com.siseg.model.TicketComentario;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.PrioridadeTicket;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.model.enumerations.TipoTicket;
import com.siseg.repository.TicketComentarioRepository;
import com.siseg.repository.TicketRepository;
import com.siseg.repository.UserRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.TicketValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceUnitTest {
    
    @Mock
    private TicketRepository ticketRepository;
    
    @Mock
    private TicketComentarioRepository ticketComentarioRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private TicketMapper ticketMapper;
    
    @Mock
    private TicketValidator ticketValidator;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private TicketService ticketService;
    
    private User user;
    private User admin;
    private Ticket ticket;
    private TicketRequestDTO requestDTO;
    private TicketResponseDTO responseDTO;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("user@test.com");
        Set<Role> userRoles = new HashSet<>();
        Role clienteRole = new Role();
        clienteRole.setRoleName(ERole.ROLE_CLIENTE);
        userRoles.add(clienteRole);
        user.setRoles(userRoles);
        
        admin = new User();
        admin.setId(2L);
        admin.setUsername("admin@test.com");
        Set<Role> adminRoles = new HashSet<>();
        Role adminRole = new Role();
        adminRole.setRoleName(ERole.ROLE_ADMIN);
        adminRoles.add(adminRole);
        admin.setRoles(adminRoles);
        
        ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitulo("Teste");
        ticket.setDescricao("Descricao teste");
        ticket.setTipo(TipoTicket.RECLAMACAO);
        ticket.setStatus(StatusTicket.ABERTO);
        ticket.setPrioridade(PrioridadeTicket.MEDIA);
        ticket.setCriadoPor(user);
        ticket.setCriadoEm(Instant.now());
        
        requestDTO = new TicketRequestDTO();
        requestDTO.setTitulo("Teste");
        requestDTO.setDescricao("Descricao teste");
        requestDTO.setTipo(TipoTicket.RECLAMACAO);
        requestDTO.setPrioridade(PrioridadeTicket.MEDIA);
        
        responseDTO = new TicketResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setTitulo("Teste");
        responseDTO.setDescricao("Descricao teste");
        responseDTO.setTipo(TipoTicket.RECLAMACAO);
        responseDTO.setStatus(StatusTicket.ABERTO);
        responseDTO.setPrioridade(PrioridadeTicket.MEDIA);
    }
    
    @Test
    void deveCriarTicketComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            
            doNothing().when(ticketValidator).validatePermissaoCriarTicket(user);
            when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
            when(ticketMapper.toResponseDTO(any(Ticket.class))).thenReturn(responseDTO);
            when(userRepository.findAll()).thenReturn(List.of(admin));
            
            TicketResponseDTO result = ticketService.criarTicket(requestDTO);
            
            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(ticketRepository).save(any(Ticket.class));
            verify(notificationService).sendEmail(anyString(), anyString(), anyString());
        }
    }
    
    @Test
    void deveBuscarTicketPorIdComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            doNothing().when(ticketValidator).validatePermissaoVisualizarTicket(ticket, user);
            when(ticketMapper.toResponseDTO(ticket)).thenReturn(responseDTO);
            
            TicketResponseDTO result = ticketService.buscarPorId(1L);
            
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }
    }
    
    @Test
    void deveLancarExcecaoQuandoTicketNaoEncontrado() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            
            when(ticketRepository.findById(1L)).thenReturn(Optional.empty());
            
            assertThrows(ResourceNotFoundException.class, () -> ticketService.buscarPorId(1L));
        }
    }
    
    @Test
    void deveListarMeusTicketsComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ticket> ticketsPage = new PageImpl<>(List.of(ticket), pageable, 1);
            
            when(ticketRepository.findByCriadoPorId(1L, pageable)).thenReturn(ticketsPage);
            when(ticketMapper.toResponseDTO(any(Ticket.class))).thenReturn(responseDTO);
            
            Page<TicketResponseDTO> result = ticketService.listarMeusTickets(pageable);
            
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }
    }
    
    @Test
    void deveAdicionarComentarioComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(false);
            
            TicketComentarioRequestDTO comentarioDTO = new TicketComentarioRequestDTO();
            comentarioDTO.setComentario("Comentario teste");
            comentarioDTO.setInterno(false);
            
            TicketComentario comentario = new TicketComentario();
            comentario.setId(1L);
            comentario.setTicket(ticket);
            comentario.setAutor(user);
            comentario.setComentario("Comentario teste");
            comentario.setInterno(false);
            
            TicketComentarioResponseDTO comentarioResponseDTO = new TicketComentarioResponseDTO();
            comentarioResponseDTO.setId(1L);
            comentarioResponseDTO.setComentario("Comentario teste");
            
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            doNothing().when(ticketValidator).validatePermissaoComentarTicket(ticket, user);
            when(ticketComentarioRepository.save(any(TicketComentario.class))).thenReturn(comentario);
            when(ticketMapper.toComentarioResponseDTO(any(TicketComentario.class))).thenReturn(comentarioResponseDTO);
            when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
            
            TicketComentarioResponseDTO result = ticketService.adicionarComentario(1L, comentarioDTO);
            
            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(ticketComentarioRepository).save(any(TicketComentario.class));
        }
    }
    
    @Test
    void deveAtualizarStatusComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(admin);
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(true);
            mockedSecurity.when(SecurityUtils::validateAdminAccess).thenAnswer(invocation -> null);
            
            ticket.setStatus(StatusTicket.EM_ANDAMENTO);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
            when(ticketMapper.toResponseDTO(any(Ticket.class))).thenReturn(responseDTO);
            
            TicketResponseDTO result = ticketService.atualizarStatus(1L, StatusTicket.EM_ANDAMENTO);
            
            assertNotNull(result);
            verify(ticketRepository).save(any(Ticket.class));
            verify(notificationService).sendEmail(anyString(), anyString(), anyString());
        }
    }
    
    @Test
    void deveResolverTicketComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(admin);
            mockedSecurity.when(SecurityUtils::isAdmin).thenReturn(true);
            mockedSecurity.when(SecurityUtils::validateAdminAccess).thenAnswer(invocation -> null);
            
            ticket.setStatus(StatusTicket.RESOLVIDO);
            responseDTO.setStatus(StatusTicket.RESOLVIDO);
            
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
            doNothing().when(ticketValidator).validateTicketAberto(ticket);
            when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
            when(ticketMapper.toResponseDTO(any(Ticket.class))).thenReturn(responseDTO);
            when(ticketComentarioRepository.save(any(TicketComentario.class))).thenReturn(new TicketComentario());
            
            TicketResponseDTO result = ticketService.resolverTicket(1L, "Resolvido");
            
            assertNotNull(result);
            assertEquals(StatusTicket.RESOLVIDO, result.getStatus());
            verify(ticketComentarioRepository).save(any(TicketComentario.class));
        }
    }
}

