package com.siseg.controller;

import com.siseg.dto.rastreamento.RastreamentoDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.repository.PedidoRepository;
import com.siseg.service.RastreamentoService;
import com.siseg.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Rastreamento", description = "Rastreamento de entregas em tempo real")
public class RastreamentoController {
    
    private final RastreamentoService rastreamentoService;
    private final PedidoRepository pedidoRepository;
    
    public RastreamentoController(RastreamentoService rastreamentoService, PedidoRepository pedidoRepository) {
        this.rastreamentoService = rastreamentoService;
        this.pedidoRepository = pedidoRepository;
    }
    
    @GetMapping("/{id}/rastreamento")
    @Operation(summary = "Consultar rastreamento do pedido")
    public ResponseEntity<RastreamentoDTO> consultarRastreamento(@PathVariable Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        User currentUser = SecurityUtils.getCurrentUser();
        
        if (!SecurityUtils.isAdmin()) {
            if (pedido.getCliente() == null || pedido.getCliente().getUser() == null ||
                !pedido.getCliente().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Você não tem permissão para consultar rastreamento deste pedido");
            }
        }
        
        RastreamentoDTO rastreamento = rastreamentoService.obterRastreamento(id);
        return ResponseEntity.ok(rastreamento);
    }
}

