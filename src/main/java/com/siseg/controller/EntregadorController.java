package com.siseg.controller;

import com.siseg.dto.entregador.EntregadorRequestDTO;
import com.siseg.dto.entregador.EntregadorResponseDTO;
import com.siseg.dto.entregador.EntregadorUpdateDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.DisponibilidadeEntregador;
import com.siseg.service.EntregadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entregadores")
@Tag(name = "Entregadores", description = "Operações de entregadores")
public class EntregadorController {

    private final EntregadorService entregadorService;

    public EntregadorController(EntregadorService entregadorService) {
        this.entregadorService = entregadorService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar entregador")
    public ResponseEntity<EntregadorResponseDTO> criarEntregador(@Valid @RequestBody EntregadorRequestDTO dto) {
        EntregadorResponseDTO response = entregadorService.criarEntregador(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar entregador por ID")
    public ResponseEntity<EntregadorResponseDTO> buscarPorId(@PathVariable String id) {
        
        try {
            Long entregadorId = Long.parseLong(id);
            EntregadorResponseDTO response = entregadorService.buscarPorId(entregadorId);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("ID inválido: " + id);
        }
    }

    @GetMapping
    @Operation(summary = "Listar todos os entregadores (Admin ou próprio entregador)")
    public ResponseEntity<Page<EntregadorResponseDTO>> listarTodos(Pageable pageable) {
        Page<EntregadorResponseDTO> response = entregadorService.listarTodos(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar entregadores por status (Admin)")
    public ResponseEntity<Page<EntregadorResponseDTO>> listarPorStatus(
            @PathVariable StatusEntregador status, 
            Pageable pageable) {
        Page<EntregadorResponseDTO> response = entregadorService.findByStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprovar entregador (Admin)")
    public ResponseEntity<EntregadorResponseDTO> aprovarEntregador(@PathVariable Long id) {
        EntregadorResponseDTO response = entregadorService.aprovarEntregador(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeitar entregador (Admin)")
    public ResponseEntity<EntregadorResponseDTO> rejeitarEntregador(@PathVariable Long id) {
        EntregadorResponseDTO response = entregadorService.rejeitarEntregador(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Atualizar dados do entregador")
    public ResponseEntity<EntregadorResponseDTO> atualizarEntregador(
            @PathVariable Long id,
            @Valid @RequestBody EntregadorUpdateDTO dto) {
        EntregadorResponseDTO response = entregadorService.atualizarEntregador(id, dto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/disponibilidade")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Atualizar disponibilidade do entregador")
    public ResponseEntity<EntregadorResponseDTO> atualizarDisponibilidade(
            @PathVariable Long id,
            @RequestParam DisponibilidadeEntregador disponibilidade) {
        EntregadorResponseDTO response = entregadorService.atualizarDisponibilidade(id, disponibilidade);
        return ResponseEntity.ok(response);
    }
}

