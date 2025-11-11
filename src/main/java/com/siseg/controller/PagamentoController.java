package com.siseg.controller;

import com.siseg.dto.pagamento.AsaasWebhookDTO;
import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
            @RequestBody(required = false) @Valid CartaoCreditoRequestDTO cartaoDTO) {
        PagamentoResponseDTO response = pagamentoService.criarPagamento(pedidoId, cartaoDTO);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pedidos/{pedidoId}")
    @Operation(summary = "Buscar pagamento por pedido")
    public ResponseEntity<PagamentoResponseDTO> buscarPagamentoPorPedido(@PathVariable Long pedidoId) {
        PagamentoResponseDTO response = pagamentoService.buscarPagamentoPorPedido(pedidoId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/webhook")
    @Operation(summary = "Webhook do Asaas para confirmação de pagamento")
    public ResponseEntity<String> webhookAsaas(
            @RequestBody AsaasWebhookDTO webhook,
            @RequestHeader("X-Signature") String signature,
            HttpServletRequest request) {
        
        try {
            String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            if (!pagamentoService.validarWebhookSignature(signature, payload)) {
                return ResponseEntity.badRequest().body("Assinatura inválida");
            }
            
            pagamentoService.processarWebhook(webhook);
            return ResponseEntity.ok("Webhook processado com sucesso");
            
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Erro ao processar webhook");
        }
    }
}
