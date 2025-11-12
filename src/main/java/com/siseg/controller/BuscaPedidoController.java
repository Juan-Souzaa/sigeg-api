package com.siseg.controller;


import com.siseg.dto.cardapio.CardapioResponseDTO;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.dto.restaurante.RestauranteBuscaDTO;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.service.PedidoService;
import com.siseg.service.RestauranteService;
import com.siseg.service.PratoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@Tag(name = "Busca e Pedidos", description = "Operações de busca de restaurantes e pedidos")
public class BuscaPedidoController {
    
    private final PedidoService pedidoService;
    private final RestauranteService restauranteService;
    private final PratoService pratoService;
    
    public BuscaPedidoController(PedidoService pedidoService,
                                 RestauranteService restauranteService, PratoService pratoService) {
        this.pedidoService = pedidoService;
        this.restauranteService = restauranteService;
        this.pratoService = pratoService;
    }
    
    @GetMapping("/restaurantes/busca")
    @Operation(summary = "Buscar restaurantes")
    public ResponseEntity<Page<RestauranteBuscaDTO>> buscarRestaurantes(
            @RequestParam(required = false) String cozinha,
            Pageable pageable) {
        Page<RestauranteBuscaDTO> response = restauranteService.buscarRestaurantes(cozinha, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/restaurantes/{id}/cardapio")
    @Operation(summary = "Buscar cardápio do restaurante")
    public ResponseEntity<CardapioResponseDTO> buscarCardapio(@PathVariable Long id, Pageable pageable) {
        CardapioResponseDTO response = pratoService.buscarCardapio(id, pageable);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/pedidos")
    @Operation(summary = "Criar pedido")
    public ResponseEntity<PedidoResponseDTO> criarPedido(
            @Valid @RequestBody PedidoRequestDTO dto) {
       
        Long clienteId = null; 
        PedidoResponseDTO response = pedidoService.criarPedido(clienteId, dto);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pedidos/meus-pedidos")
    @Operation(summary = "Listar meus pedidos com filtros")
    public ResponseEntity<Page<PedidoResponseDTO>> listarMeusPedidos(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            @RequestParam(required = false) Long restauranteId,
            Pageable pageable) {
        Page<PedidoResponseDTO> response = pedidoService.listarMeusPedidos(
            status, dataInicio, dataFim, restauranteId, pageable
        );
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pedidos/{id}")
    @Operation(summary = "Buscar pedido por ID")
    public ResponseEntity<PedidoResponseDTO> buscarPedido(@PathVariable Long id) {
        PedidoResponseDTO response = pedidoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/pedidos/{id}/confirmar")
    @Operation(summary = "Confirmar pedido")
    public ResponseEntity<PedidoResponseDTO> confirmarPedido(@PathVariable Long id) {
        PedidoResponseDTO response = pedidoService.confirmarPedido(id);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/pedidos/{id}/preparando")
    @Operation(summary = "Marcar pedido como preparando (Restaurante)")
    public ResponseEntity<PedidoResponseDTO> marcarComoPreparando(@PathVariable Long id) {
        PedidoResponseDTO response = pedidoService.marcarComoPreparando(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/restaurantes/pedidos/meus-pedidos")
    @PreAuthorize("hasAnyRole('RESTAURANTE', 'ADMIN')")
    @Operation(summary = "Listar pedidos do restaurante com filtros")
    public ResponseEntity<Page<PedidoResponseDTO>> listarPedidosRestaurante(
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dataFim,
            Pageable pageable) {
        Page<PedidoResponseDTO> response = pedidoService.listarPedidosRestaurante(
            status, dataInicio, dataFim, pageable
        );
        return ResponseEntity.ok(response);
    }
    
}
