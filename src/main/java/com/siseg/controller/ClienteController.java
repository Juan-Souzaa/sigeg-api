package com.siseg.controller;

import com.siseg.dto.cliente.ClienteRequestDTO;
import com.siseg.dto.cliente.ClienteResponseDTO;
import com.siseg.model.Cliente;
import com.siseg.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Operações de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @Operation(summary = "Cadastrar cliente")
    public ResponseEntity<ClienteResponseDTO> criarCliente(@Valid @RequestBody ClienteRequestDTO dto) {
        ClienteResponseDTO response = clienteService.criarCliente(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        ClienteResponseDTO response = clienteService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar clientes")
    public ResponseEntity<Page<ClienteResponseDTO>> listarTodos(Pageable pageable) {
        Page<ClienteResponseDTO> response = clienteService.listarTodos(pageable);
        return ResponseEntity.ok(response);
    }
}
