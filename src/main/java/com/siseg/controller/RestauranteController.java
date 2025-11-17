package com.siseg.controller;

import com.siseg.dto.AtualizarSenhaDTO;
import com.siseg.dto.restaurante.AtualizarRaioEntregaDTO;
import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.dto.restaurante.RestauranteUpdateDTO;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.service.RestauranteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/restaurantes")
@Tag(name = "Restaurantes", description = "Operações de restaurantes")
public class RestauranteController {
    
    private final RestauranteService restauranteService;
    
    public RestauranteController(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }
    
    @PostMapping
    @Operation(summary = "Cadastrar restaurante")
    public ResponseEntity<RestauranteResponseDTO> criarRestaurante(@Valid @RequestBody RestauranteRequestDTO dto) {
        RestauranteResponseDTO response = restauranteService.criarRestaurante(dto);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Buscar restaurante por ID")
    public ResponseEntity<RestauranteResponseDTO> buscarPorId(@PathVariable Long id) {
        RestauranteResponseDTO response = restauranteService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aprovar restaurante (Admin)")
    public ResponseEntity<RestauranteResponseDTO> aprovarRestaurante(@PathVariable Long id) {
        RestauranteResponseDTO response = restauranteService.aprovarRestaurante(id);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeitar restaurante (Admin)")
    public ResponseEntity<RestauranteResponseDTO> rejeitarRestaurante(@PathVariable Long id) {
        RestauranteResponseDTO response = restauranteService.rejeitarRestaurante(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Listar todos os restaurantes")
    public ResponseEntity<Page<RestauranteResponseDTO>> listarTodos(Pageable pageable) {
        Page<RestauranteResponseDTO> response = restauranteService.listarTodos(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar restaurantes por status (Admin)")
    public ResponseEntity<Page<RestauranteResponseDTO>> listarPorStatus(@PathVariable StatusRestaurante status, Pageable pageable) {
        Page<RestauranteResponseDTO> response = restauranteService.listarPorStatus(status, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar restaurante")
    public ResponseEntity<RestauranteResponseDTO> atualizarRestaurante(
            @PathVariable Long id,
            @Valid @RequestBody RestauranteUpdateDTO dto) {
        RestauranteResponseDTO response = restauranteService.atualizarRestaurante(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir restaurante (soft delete)")
    public ResponseEntity<Void> excluirRestaurante(@PathVariable Long id) {
        restauranteService.excluirRestaurante(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/senha")
    @Operation(summary = "Atualizar senha do restaurante")
    public ResponseEntity<Void> atualizarSenha(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarSenhaDTO dto) {
        restauranteService.atualizarSenha(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/raio-entrega")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANTE')")
    @Operation(summary = "Atualizar raio de entrega do restaurante")
    public ResponseEntity<Void> atualizarRaioEntrega(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarRaioEntregaDTO dto) {
        restauranteService.atualizarRaioEntrega(id, dto.getRaioEntregaKm());
        return ResponseEntity.noContent().build();
    }
}
