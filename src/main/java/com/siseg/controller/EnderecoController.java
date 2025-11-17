package com.siseg.controller;

import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.EnderecoResponseDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Restaurante;
import com.siseg.service.EnderecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Endereços", description = "Operações de gerenciamento de endereços")
public class EnderecoController {

    private final EnderecoService enderecoService;

    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @GetMapping("/clientes/{clienteId}/enderecos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Listar endereços do cliente")
    public ResponseEntity<List<EnderecoResponseDTO>> listarEnderecosCliente(@PathVariable Long clienteId) {
        List<EnderecoResponseDTO> response = enderecoService.listarEnderecosCliente(clienteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurantes/{restauranteId}/enderecos")
    @PreAuthorize("hasAnyRole('RESTAURANTE', 'ADMIN')")
    @Operation(summary = "Listar endereços do restaurante")
    public ResponseEntity<List<EnderecoResponseDTO>> listarEnderecosRestaurante(@PathVariable Long restauranteId) {
        List<EnderecoResponseDTO> response = enderecoService.listarEnderecosRestaurante(restauranteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/clientes/{clienteId}/enderecos/{enderecoId}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Buscar endereço específico do cliente")
    public ResponseEntity<EnderecoResponseDTO> buscarEnderecoCliente(
            @PathVariable Long clienteId, 
            @PathVariable Long enderecoId) {
        EnderecoResponseDTO response = enderecoService.buscarEnderecoPorIdCliente(enderecoId, clienteId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurantes/{restauranteId}/enderecos/{enderecoId}")
    @PreAuthorize("hasAnyRole('RESTAURANTE', 'ADMIN')")
    @Operation(summary = "Buscar endereço específico do restaurante")
    public ResponseEntity<EnderecoResponseDTO> buscarEnderecoRestaurante(
            @PathVariable Long restauranteId, 
            @PathVariable Long enderecoId) {
        EnderecoResponseDTO response = enderecoService.buscarEnderecoPorIdRestaurante(enderecoId, restauranteId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clientes/{clienteId}/enderecos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Adicionar novo endereço ao cliente")
    public ResponseEntity<EnderecoResponseDTO> adicionarEnderecoCliente(
            @PathVariable Long clienteId,
            @Valid @RequestBody EnderecoRequestDTO dto) {
        Cliente cliente = enderecoService.buscarClienteParaEndereco(clienteId);
        Endereco endereco = enderecoService.criarEndereco(dto, cliente);
        EnderecoResponseDTO response = enderecoService.buscarEnderecoPorIdCliente(endereco.getId(), clienteId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restaurantes/{restauranteId}/enderecos")
    @PreAuthorize("hasAnyRole('RESTAURANTE', 'ADMIN')")
    @Operation(summary = "Adicionar novo endereço ao restaurante")
    public ResponseEntity<EnderecoResponseDTO> adicionarEnderecoRestaurante(
            @PathVariable Long restauranteId,
            @Valid @RequestBody EnderecoRequestDTO dto) {
        Restaurante restaurante = enderecoService.buscarRestauranteParaEndereco(restauranteId);
        Endereco endereco = enderecoService.criarEndereco(dto, restaurante);
        EnderecoResponseDTO response = enderecoService.buscarEnderecoPorIdRestaurante(endereco.getId(), restauranteId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/clientes/{clienteId}/enderecos/{enderecoId}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Atualizar endereço do cliente")
    public ResponseEntity<EnderecoResponseDTO> atualizarEnderecoCliente(
            @PathVariable Long clienteId,
            @PathVariable Long enderecoId,
            @Valid @RequestBody EnderecoRequestDTO dto) {
        EnderecoResponseDTO response = enderecoService.atualizarEnderecoCliente(enderecoId, dto, clienteId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/restaurantes/{restauranteId}/enderecos/{enderecoId}")
    @PreAuthorize("hasAnyRole('RESTAURANTE', 'ADMIN')")
    @Operation(summary = "Atualizar endereço do restaurante")
    public ResponseEntity<EnderecoResponseDTO> atualizarEnderecoRestaurante(
            @PathVariable Long restauranteId,
            @PathVariable Long enderecoId,
            @Valid @RequestBody EnderecoRequestDTO dto) {
        EnderecoResponseDTO response = enderecoService.atualizarEnderecoRestaurante(enderecoId, dto, restauranteId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/clientes/{clienteId}/enderecos/{enderecoId}")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Excluir endereço do cliente")
    public ResponseEntity<Void> excluirEnderecoCliente(
            @PathVariable Long clienteId,
            @PathVariable Long enderecoId) {
        enderecoService.excluirEnderecoCliente(enderecoId, clienteId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/restaurantes/{restauranteId}/enderecos/{enderecoId}")
    @PreAuthorize("hasAnyRole('RESTAURANTE', 'ADMIN')")
    @Operation(summary = "Excluir endereço do restaurante")
    public ResponseEntity<Void> excluirEnderecoRestaurante(
            @PathVariable Long restauranteId,
            @PathVariable Long enderecoId) {
        enderecoService.excluirEnderecoRestaurante(enderecoId, restauranteId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/clientes/{clienteId}/enderecos/{enderecoId}/principal")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Definir endereço como principal (cliente)")
    public ResponseEntity<EnderecoResponseDTO> definirEnderecoPrincipalCliente(
            @PathVariable Long clienteId,
            @PathVariable Long enderecoId) {
        enderecoService.definirEnderecoPrincipalCliente(enderecoId, clienteId);
        EnderecoResponseDTO response = enderecoService.buscarEnderecoPorIdCliente(enderecoId, clienteId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/restaurantes/{restauranteId}/enderecos/{enderecoId}/principal")
    @PreAuthorize("hasAnyRole('RESTAURANTE', 'ADMIN')")
    @Operation(summary = "Definir endereço como principal (restaurante)")
    public ResponseEntity<EnderecoResponseDTO> definirEnderecoPrincipalRestaurante(
            @PathVariable Long restauranteId,
            @PathVariable Long enderecoId) {
        enderecoService.definirEnderecoPrincipalRestaurante(enderecoId, restauranteId);
        EnderecoResponseDTO response = enderecoService.buscarEnderecoPorIdRestaurante(enderecoId, restauranteId);
        return ResponseEntity.ok(response);
    }
}