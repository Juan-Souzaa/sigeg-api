package com.siseg.controller;

import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.dto.pagamento.ReembolsoRequestDTO;
import com.siseg.service.PagamentoService;
import com.siseg.util.HttpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamentos")
@Tag(name = "Pagamentos", description = "Operações de pagamento")
public class PagamentoController {
    
    private final PagamentoService pagamentoService;
    
    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }
    
    @PostMapping("/pedidos/{pedidoId}")
    @Operation(summary = "Criar pagamento para pedido")
    public ResponseEntity<PagamentoResponseDTO> criarPagamento(
            @PathVariable Long pedidoId,
            @RequestBody(required = false) @Valid CartaoCreditoRequestDTO cartaoDTO,
            HttpServletRequest request) {
        String remoteIp = HttpUtils.getClientIpAddress(request);
        PagamentoResponseDTO response = pagamentoService.criarPagamento(pedidoId, cartaoDTO, remoteIp);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pedidos/{pedidoId}")
    @Operation(summary = "Buscar pagamento por pedido")
    public ResponseEntity<PagamentoResponseDTO> buscarPagamentoPorPedido(@PathVariable Long pedidoId) {
        PagamentoResponseDTO response = pagamentoService.buscarPagamentoPorPedido(pedidoId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/pedidos/{pedidoId}/reembolso")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANTE')")
    @Operation(summary = "Estornar pagamento de um pedido")
    public ResponseEntity<PagamentoResponseDTO> estornarPagamento(
            @PathVariable Long pedidoId,
            @RequestBody @Valid ReembolsoRequestDTO request) {
        PagamentoResponseDTO response = pagamentoService.processarReembolso(pedidoId, request.getMotivo());
        return ResponseEntity.ok(response);
    }
}
