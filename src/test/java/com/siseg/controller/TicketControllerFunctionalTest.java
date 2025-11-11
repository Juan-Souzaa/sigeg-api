package com.siseg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.ticket.TicketComentarioRequestDTO;
import com.siseg.dto.ticket.TicketComentarioResponseDTO;
import com.siseg.dto.ticket.TicketDetalhadoResponseDTO;
import com.siseg.dto.ticket.TicketRequestDTO;
import com.siseg.dto.ticket.TicketResponseDTO;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.PrioridadeTicket;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.model.enumerations.TipoTicket;
import com.siseg.service.TicketService;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerFunctionalTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TicketService ticketService;
    
    @Autowired
    private TestJwtUtil testJwtUtil;
    
    private String clienteToken;
    private String adminToken;
    private TicketRequestDTO ticketRequestDTO;
    private TicketResponseDTO ticketResponseDTO;
    private TicketDetalhadoResponseDTO ticketDetalhadoResponseDTO;
    private TicketComentarioRequestDTO comentarioRequestDTO;
    private TicketComentarioResponseDTO comentarioResponseDTO;
    
    @BeforeEach
    void setUp() {
        clienteToken = testJwtUtil.generateTokenForUser("cliente", ERole.ROLE_CLIENTE);
        adminToken = testJwtUtil.generateTokenForUser("admin", ERole.ROLE_ADMIN);
        
        ticketRequestDTO = new TicketRequestDTO();
        ticketRequestDTO.setTitulo("Problema no pedido");
        ticketRequestDTO.setDescricao("Meu pedido nao chegou");
        ticketRequestDTO.setTipo(TipoTicket.RECLAMACAO);
        ticketRequestDTO.setPrioridade(PrioridadeTicket.ALTA);
        
        ticketResponseDTO = new TicketResponseDTO();
        ticketResponseDTO.setId(1L);
        ticketResponseDTO.setTitulo("Problema no pedido");
        ticketResponseDTO.setDescricao("Meu pedido nao chegou");
        ticketResponseDTO.setTipo(TipoTicket.RECLAMACAO);
        ticketResponseDTO.setStatus(StatusTicket.ABERTO);
        ticketResponseDTO.setPrioridade(PrioridadeTicket.ALTA);
        
        ticketDetalhadoResponseDTO = new TicketDetalhadoResponseDTO();
        ticketDetalhadoResponseDTO.setTicket(ticketResponseDTO);
        ticketDetalhadoResponseDTO.setComentarios(List.of());
        
        comentarioRequestDTO = new TicketComentarioRequestDTO();
        comentarioRequestDTO.setComentario("Vou verificar");
        comentarioRequestDTO.setInterno(false);
        
        comentarioResponseDTO = new TicketComentarioResponseDTO();
        comentarioResponseDTO.setId(1L);
        comentarioResponseDTO.setComentario("Vou verificar");
        comentarioResponseDTO.setInterno(false);
    }
    
    @Test
    void deveCriarTicketComSucesso() throws Exception {
        when(ticketService.criarTicket(any(TicketRequestDTO.class))).thenReturn(ticketResponseDTO);
        
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticketRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.titulo").value("Problema no pedido"));
    }
    
    @Test
    void deveListarMeusTicketsComSucesso() throws Exception {
        Page<TicketResponseDTO> page = new PageImpl<>(List.of(ticketResponseDTO), PageRequest.of(0, 10), 1);
        when(ticketService.listarMeusTickets(any())).thenReturn(page);
        
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + clienteToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    void deveBuscarTicketPorIdComSucesso() throws Exception {
        when(ticketService.buscarPorId(1L)).thenReturn(ticketResponseDTO);
        
        mockMvc.perform(get("/api/tickets/1")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
    
    @Test
    void deveBuscarTicketDetalhadoComSucesso() throws Exception {
        when(ticketService.buscarDetalhadoPorId(1L)).thenReturn(ticketDetalhadoResponseDTO);
        
        mockMvc.perform(get("/api/tickets/1/detalhes")
                        .header("Authorization", "Bearer " + clienteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.id").value(1L))
                .andExpect(jsonPath("$.comentarios").isArray());
    }
    
    @Test
    void deveAdicionarComentarioComSucesso() throws Exception {
        when(ticketService.adicionarComentario(eq(1L), any(TicketComentarioRequestDTO.class)))
                .thenReturn(comentarioResponseDTO);
        
        mockMvc.perform(post("/api/tickets/1/comentarios")
                        .header("Authorization", "Bearer " + clienteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comentarioRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.comentario").value("Vou verificar"));
    }
    
    @Test
    void deveAtribuirTicketComSucesso() throws Exception {
        ticketResponseDTO.setStatus(StatusTicket.EM_ANDAMENTO);
        when(ticketService.atribuirTicket(1L, 2L)).thenReturn(ticketResponseDTO);
        
        mockMvc.perform(patch("/api/tickets/1/atribuir")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("adminId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }
    
    @Test
    void deveAtualizarStatusComSucesso() throws Exception {
        ticketResponseDTO.setStatus(StatusTicket.EM_ANDAMENTO);
        when(ticketService.atualizarStatus(1L, StatusTicket.EM_ANDAMENTO)).thenReturn(ticketResponseDTO);
        
        mockMvc.perform(patch("/api/tickets/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "EM_ANDAMENTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }
    
    @Test
    void deveResolverTicketComSucesso() throws Exception {
        ticketResponseDTO.setStatus(StatusTicket.RESOLVIDO);
        when(ticketService.resolverTicket(1L, "Resolvido")).thenReturn(ticketResponseDTO);
        
        mockMvc.perform(post("/api/tickets/1/resolver")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("resolucao", "Resolvido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDO"));
    }
    
    @Test
    void deveListarTodosTicketsComoAdmin() throws Exception {
        Page<TicketResponseDTO> page = new PageImpl<>(List.of(ticketResponseDTO), PageRequest.of(0, 10), 1);
        when(ticketService.listarTodos(any())).thenReturn(page);
        
        mockMvc.perform(get("/api/tickets/admin/todos")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
    
    @Test
    void deveListarTicketsPorStatusComoAdmin() throws Exception {
        Page<TicketResponseDTO> page = new PageImpl<>(List.of(ticketResponseDTO), PageRequest.of(0, 10), 1);
        when(ticketService.listarPorStatus(eq(StatusTicket.ABERTO), any())).thenReturn(page);
        
        mockMvc.perform(get("/api/tickets/admin/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "ABERTO")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
    
    @Test
    void deveListarTicketsPorTipoComoAdmin() throws Exception {
        Page<TicketResponseDTO> page = new PageImpl<>(List.of(ticketResponseDTO), PageRequest.of(0, 10), 1);
        when(ticketService.listarPorTipo(eq(TipoTicket.RECLAMACAO), any())).thenReturn(page);
        
        mockMvc.perform(get("/api/tickets/admin/tipo")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("tipo", "RECLAMACAO")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}

