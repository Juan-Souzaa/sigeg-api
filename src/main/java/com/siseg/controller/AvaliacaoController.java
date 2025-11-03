package com.siseg.controller;

import com.siseg.dto.avaliacao.AvaliacaoRequestDTO;
import com.siseg.dto.avaliacao.AvaliacaoResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoResumoDTO;
import com.siseg.dto.avaliacao.AvaliacaoRestauranteResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoEntregadorResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoResumoEntregadorDTO;
import com.siseg.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/avaliacoes")
@Tag(name = "Avaliações", description = "Operações para criar, editar e consultar avaliações")
public class AvaliacaoController {
    
    private final AvaliacaoService avaliacaoService;
    
    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }
    
    @PostMapping("/pedidos/{pedidoId}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Criar avaliação para um pedido")
    public ResponseEntity<AvaliacaoResponseDTO> criarAvaliacao(
            @PathVariable Long pedidoId,
            @Valid @RequestBody AvaliacaoRequestDTO dto) {
        AvaliacaoResponseDTO response = avaliacaoService.criarAvaliacao(pedidoId, dto);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Editar avaliação existente")
    public ResponseEntity<AvaliacaoResponseDTO> editarAvaliacao(
            @PathVariable Long id,
            @Valid @RequestBody AvaliacaoRequestDTO dto) {
        AvaliacaoResponseDTO response = avaliacaoService.editarAvaliacao(id, dto);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pedidos/{pedidoId}")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMIN')")
    @Operation(summary = "Buscar avaliação por pedido")
    public ResponseEntity<AvaliacaoResponseDTO> buscarAvaliacaoPorPedido(@PathVariable Long pedidoId) {
        AvaliacaoResponseDTO response = avaliacaoService.buscarAvaliacaoPorPedido(pedidoId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/restaurantes/{restauranteId}")
    @Operation(summary = "Listar avaliações de um restaurante")
    public ResponseEntity<Page<AvaliacaoRestauranteResponseDTO>> listarAvaliacoesPorRestaurante(
            @PathVariable Long restauranteId,
            Pageable pageable) {
        Page<AvaliacaoRestauranteResponseDTO> response = avaliacaoService.listarAvaliacoesPorRestaurante(restauranteId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/restaurantes/{restauranteId}/resumo")
    @Operation(summary = "Obter resumo de avaliações de um restaurante (média e total)")
    public ResponseEntity<AvaliacaoResumoDTO> obterResumoRestaurante(@PathVariable Long restauranteId) {
        AvaliacaoResumoDTO response = avaliacaoService.obterResumoRestaurante(restauranteId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/entregadores/{entregadorId}")
    @Operation(summary = "Listar avaliações de um entregador")
    public ResponseEntity<Page<AvaliacaoEntregadorResponseDTO>> listarAvaliacoesPorEntregador(
            @PathVariable Long entregadorId,
            Pageable pageable) {
        Page<AvaliacaoEntregadorResponseDTO> response = avaliacaoService.listarAvaliacoesPorEntregador(entregadorId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/entregadores/{entregadorId}/resumo")
    @Operation(summary = "Obter resumo de avaliações de um entregador (média e total)")
    public ResponseEntity<AvaliacaoResumoEntregadorDTO> obterResumoEntregador(@PathVariable Long entregadorId) {
        AvaliacaoResumoEntregadorDTO response = avaliacaoService.obterResumoEntregador(entregadorId);
        return ResponseEntity.ok(response);
    }
}

