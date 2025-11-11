package com.siseg.controller;

import com.siseg.dto.ticket.TicketComentarioRequestDTO;
import com.siseg.dto.ticket.TicketComentarioResponseDTO;
import com.siseg.dto.ticket.TicketDetalhadoResponseDTO;
import com.siseg.dto.ticket.TicketRequestDTO;
import com.siseg.dto.ticket.TicketResponseDTO;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.model.enumerations.TipoTicket;
import com.siseg.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Operacoes de tickets de suporte")
public class TicketController {
    
    private final TicketService ticketService;
    
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }
    
    @PostMapping
    @Operation(summary = "Criar ticket")
    public ResponseEntity<TicketResponseDTO> criarTicket(@Valid @RequestBody TicketRequestDTO dto) {
        TicketResponseDTO response = ticketService.criarTicket(dto);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Listar tickets")
    public ResponseEntity<Page<TicketResponseDTO>> listarTickets(Pageable pageable) {
        Page<TicketResponseDTO> response = ticketService.listarMeusTickets(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Buscar ticket por ID")
    public ResponseEntity<TicketResponseDTO> buscarTicket(@PathVariable Long id) {
        TicketResponseDTO response = ticketService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/detalhes")
    @Operation(summary = "Buscar ticket com comentarios")
    public ResponseEntity<TicketDetalhadoResponseDTO> buscarTicketDetalhado(@PathVariable Long id) {
        TicketDetalhadoResponseDTO response = ticketService.buscarDetalhadoPorId(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/comentarios")
    @Operation(summary = "Adicionar comentario ao ticket")
    public ResponseEntity<TicketComentarioResponseDTO> adicionarComentario(
            @PathVariable Long id,
            @Valid @RequestBody TicketComentarioRequestDTO dto) {
        TicketComentarioResponseDTO response = ticketService.adicionarComentario(id, dto);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/atribuir")
    @Operation(summary = "Atribuir ticket a admin")
    public ResponseEntity<TicketResponseDTO> atribuirTicket(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        TicketResponseDTO response = ticketService.atribuirTicket(id, adminId);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status do ticket")
    public ResponseEntity<TicketResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @RequestParam StatusTicket status) {
        TicketResponseDTO response = ticketService.atualizarStatus(id, status);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/resolver")
    @Operation(summary = "Resolver ticket")
    public ResponseEntity<TicketResponseDTO> resolverTicket(
            @PathVariable Long id,
            @RequestParam String resolucao) {
        TicketResponseDTO response = ticketService.resolverTicket(id, resolucao);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/admin/todos")
    @Operation(summary = "Listar todos os tickets (apenas admin)")
    public ResponseEntity<Page<TicketResponseDTO>> listarTodos(Pageable pageable) {
        Page<TicketResponseDTO> response = ticketService.listarTodos(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/admin/status")
    @Operation(summary = "Listar tickets por status (apenas admin)")
    public ResponseEntity<Page<TicketResponseDTO>> listarPorStatus(
            @RequestParam StatusTicket status,
            Pageable pageable) {
        Page<TicketResponseDTO> response = ticketService.listarPorStatus(status, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/admin/tipo")
    @Operation(summary = "Listar tickets por tipo (apenas admin)")
    public ResponseEntity<Page<TicketResponseDTO>> listarPorTipo(
            @RequestParam TipoTicket tipo,
            Pageable pageable) {
        Page<TicketResponseDTO> response = ticketService.listarPorTipo(tipo, pageable);
        return ResponseEntity.ok(response);
    }
}

