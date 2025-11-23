package com.siseg.controller;

import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entregadores")
@Tag(name = "Pedidos do Entregador", description = "Operações de pedidos para entregadores")
public class EntregadorPedidoController {
    
    private final PedidoService pedidoService;
    
    public EntregadorPedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }
    
    @GetMapping("/entregas")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Listar entregas ativas do entregador autenticado")
    public ResponseEntity<Page<PedidoResponseDTO>> listarEntregasAtivas(Pageable pageable) {
        Page<PedidoResponseDTO> response = pedidoService.listarEntregasAtivas(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/entregas/historico")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Listar histórico de entregas concluídas do entregador autenticado")
    public ResponseEntity<Page<PedidoResponseDTO>> listarHistoricoEntregas(Pageable pageable) {
        Page<PedidoResponseDTO> response = pedidoService.listarHistoricoEntregas(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pedidos/disponiveis")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Listar pedidos disponíveis para entrega")
    public ResponseEntity<Page<PedidoResponseDTO>> listarPedidosDisponiveis(Pageable pageable) {
        Page<PedidoResponseDTO> response = pedidoService.listarPedidosDisponiveis(pageable);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/pedidos/{id}/aceitar")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Aceitar pedido para entrega")
    public ResponseEntity<PedidoResponseDTO> aceitarPedido(@PathVariable Long id) {
        PedidoResponseDTO response = pedidoService.aceitarPedido(id);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/pedidos/{id}/saiu-entrega")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Marcar pedido como saiu para entrega")
    public ResponseEntity<PedidoResponseDTO> marcarSaiuEntrega(@PathVariable Long id) {
        PedidoResponseDTO response = pedidoService.marcarSaiuEntrega(id);
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/pedidos/{id}/entregue")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Marcar pedido como entregue")
    public ResponseEntity<PedidoResponseDTO> marcarComoEntregue(@PathVariable Long id) {
        PedidoResponseDTO response = pedidoService.marcarComoEntregue(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/pedidos/{id}/recusar")
    @PreAuthorize("hasRole('ENTREGADOR')")
    @Operation(summary = "Recusar pedido (opcional - apenas log)")
    public ResponseEntity<Void> recusarPedido(@PathVariable Long id) {
        pedidoService.recusarPedido(id);
        return ResponseEntity.ok().build();
    }
}

