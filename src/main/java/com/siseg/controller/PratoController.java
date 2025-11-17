package com.siseg.controller;

import com.siseg.dto.prato.PratoRequestDTO;
import com.siseg.dto.prato.PratoResponseDTO;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.service.PratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/restaurantes/{restauranteId}/pratos")
@Tag(name = "Pratos", description = "Operações de pratos")
public class PratoController {
    
    private final PratoService pratoService;
    
    public PratoController(PratoService pratoService) {
        this.pratoService = pratoService;
    }
    
    @PostMapping
    @Operation(summary = "Criar prato")
    public ResponseEntity<PratoResponseDTO> criarPrato(
            @PathVariable Long restauranteId,
            @Valid @ModelAttribute PratoRequestDTO dto) {
        PratoResponseDTO response = pratoService.criarPrato(restauranteId, dto);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar prato")
    public ResponseEntity<PratoResponseDTO> atualizarPrato(
            @PathVariable Long id,
            @Valid @ModelAttribute PratoRequestDTO dto) {
        PratoResponseDTO response = pratoService.atualizarPrato(id, dto);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/disponibilidade")
    @Operation(summary = "Alternar disponibilidade do prato")
    public ResponseEntity<PratoResponseDTO> alternarDisponibilidade(@PathVariable Long id) {
        PratoResponseDTO response = pratoService.alternarDisponibilidade(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Listar pratos do restaurante")
    public ResponseEntity<Page<PratoResponseDTO>> listarPratos(
            @PathVariable Long restauranteId,
            @RequestParam(required = false) CategoriaMenu categoria,
            @RequestParam(required = false) Boolean disponivel,
            Pageable pageable) {
        Page<PratoResponseDTO> response = pratoService.listarPorRestaurante(restauranteId, categoria, disponivel, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar prato por ID")
    public ResponseEntity<PratoResponseDTO> buscarPorId(
            @PathVariable Long restauranteId,
            @PathVariable Long id) {
        PratoResponseDTO response = pratoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir prato")
    public ResponseEntity<Void> excluirPrato(
            @PathVariable Long restauranteId,
            @PathVariable Long id) {
        pratoService.excluirPrato(id, restauranteId);
        return ResponseEntity.noContent().build();
    }
}
