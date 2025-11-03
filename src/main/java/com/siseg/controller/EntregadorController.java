package com.siseg.controller;

import com.siseg.dto.entregador.EntregadorRequestDTO;
import com.siseg.dto.entregador.EntregadorResponseDTO;
import com.siseg.model.enumerations.StatusEntregador;
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
    public ResponseEntity<EntregadorResponseDTO> buscarPorId(@PathVariable Long id) {
        EntregadorResponseDTO response = entregadorService.buscarPorId(id);
        return ResponseEntity.ok(response);
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
}

