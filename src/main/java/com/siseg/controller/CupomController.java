package com.siseg.controller;

import com.siseg.dto.cupom.CupomRequestDTO;
import com.siseg.dto.cupom.CupomResponseDTO;
import com.siseg.service.CupomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cupons")
@Tag(name = "Cupons", description = "Operações de cupons de desconto")
public class CupomController {

    private final CupomService cupomService;

    public CupomController(CupomService cupomService) {
        this.cupomService = cupomService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar cupom de desconto (apenas admin)")
    public ResponseEntity<CupomResponseDTO> criarCupom(@Valid @RequestBody CupomRequestDTO dto) {
        CupomResponseDTO response = cupomService.criarCupom(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Buscar cupom por código")
    public ResponseEntity<CupomResponseDTO> buscarPorCodigo(@PathVariable String codigo) {
        CupomResponseDTO response = cupomService.buscarPorCodigoDTO(codigo);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar cupons ativos (apenas admin)")
    public ResponseEntity<Page<CupomResponseDTO>> listarCuponsAtivos(Pageable pageable) {
        Page<CupomResponseDTO> response = cupomService.listarCuponsAtivos(pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar cupom (apenas admin)")
    public ResponseEntity<CupomResponseDTO> desativarCupom(@PathVariable Long id) {
        CupomResponseDTO response = cupomService.desativarCupom(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disponiveis")
    @Operation(summary = "Listar cupons disponíveis para clientes")
    public ResponseEntity<Page<CupomResponseDTO>> listarCuponsDisponiveis(Pageable pageable) {
        Page<CupomResponseDTO> response = cupomService.listarCuponsDisponiveis(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{codigo}/validar")
    @Operation(summary = "Validar cupom antes de aplicar")
    public ResponseEntity<CupomResponseDTO> validarCupom(@PathVariable String codigo) {
        CupomResponseDTO response = cupomService.validarCupom(codigo);
        return ResponseEntity.ok(response);
    }
}

