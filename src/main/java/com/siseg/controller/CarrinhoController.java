package com.siseg.controller;

import com.siseg.dto.carrinho.AplicarCupomRequestDTO;
import com.siseg.dto.carrinho.CarrinhoItemRequestDTO;
import com.siseg.dto.carrinho.CarrinhoResponseDTO;
import com.siseg.service.CarrinhoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrinho")
@Tag(name = "Carrinho", description = "Operações de carrinho de compras")
@PreAuthorize("hasRole('CLIENTE')")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }

    @GetMapping
    @Operation(summary = "Obter carrinho ativo do cliente")
    public ResponseEntity<CarrinhoResponseDTO> obterCarrinho() {
        CarrinhoResponseDTO response = carrinhoService.obterCarrinhoAtivo();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/itens")
    @Operation(summary = "Adicionar item ao carrinho")
    public ResponseEntity<CarrinhoResponseDTO> adicionarItem(@Valid @RequestBody CarrinhoItemRequestDTO dto) {
        CarrinhoResponseDTO response = carrinhoService.adicionarItem(dto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/itens/{id}")
    @Operation(summary = "Atualizar quantidade de um item")
    public ResponseEntity<CarrinhoResponseDTO> atualizarQuantidade(
            @PathVariable Long id,
            @RequestParam Integer quantidade) {
        CarrinhoResponseDTO response = carrinhoService.atualizarQuantidade(id, quantidade);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/itens/{id}")
    @Operation(summary = "Remover item do carrinho")
    public ResponseEntity<CarrinhoResponseDTO> removerItem(@PathVariable Long id) {
        CarrinhoResponseDTO response = carrinhoService.removerItem(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cupom")
    @Operation(summary = "Aplicar cupom de desconto")
    public ResponseEntity<CarrinhoResponseDTO> aplicarCupom(@Valid @RequestBody AplicarCupomRequestDTO dto) {
        CarrinhoResponseDTO response = carrinhoService.aplicarCupom(dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cupom")
    @Operation(summary = "Remover cupom do carrinho")
    public ResponseEntity<CarrinhoResponseDTO> removerCupom() {
        CarrinhoResponseDTO response = carrinhoService.removerCupom();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Limpar carrinho")
    public ResponseEntity<Void> limparCarrinho() {
        carrinhoService.limparCarrinho();
        return ResponseEntity.ok().build();
    }
}

