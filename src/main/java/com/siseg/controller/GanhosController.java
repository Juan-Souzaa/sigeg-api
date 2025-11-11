package com.siseg.controller;

import com.siseg.dto.ganhos.GanhosEntregadorDTO;
import com.siseg.dto.ganhos.GanhosPorEntregaDTO;
import com.siseg.dto.ganhos.GanhosRestauranteDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Entregador;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.Periodo;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.GanhosService;
import com.siseg.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Ganhos", description = "Operações de ganhos para restaurantes e entregadores")
public class GanhosController {

    private final GanhosService ganhosService;
    private final RestauranteRepository restauranteRepository;
    private final EntregadorRepository entregadorRepository;

    public GanhosController(GanhosService ganhosService, RestauranteRepository restauranteRepository,
                            EntregadorRepository entregadorRepository) {
        this.ganhosService = ganhosService;
        this.restauranteRepository = restauranteRepository;
        this.entregadorRepository = entregadorRepository;
    }

    @GetMapping("/restaurantes/{id}/ganhos")
    @PreAuthorize("hasAnyRole('RESTAURANTE', 'ADMIN')")
    @Operation(summary = "Obter ganhos do restaurante")
    public ResponseEntity<GanhosRestauranteDTO> obterGanhosRestaurante(
            @PathVariable Long id,
            @RequestParam(defaultValue = "MES") Periodo periodo) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
        SecurityUtils.validateRestauranteOwnership(restaurante);
        
        GanhosRestauranteDTO response = ganhosService.calcularGanhosRestaurante(id, periodo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entregadores/{id}/ganhos")
    @PreAuthorize("hasAnyRole('ENTREGADOR', 'ADMIN')")
    @Operation(summary = "Obter ganhos do entregador")
    public ResponseEntity<GanhosEntregadorDTO> obterGanhosEntregador(
            @PathVariable Long id,
            @RequestParam(defaultValue = "MES") Periodo periodo) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado"));
        SecurityUtils.validateEntregadorOwnership(entregador);
        
        GanhosEntregadorDTO response = ganhosService.calcularGanhosEntregador(id, periodo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/entregadores/{id}/ganhos/entregas")
    @PreAuthorize("hasAnyRole('ENTREGADOR', 'ADMIN')")
    @Operation(summary = "Listar ganhos por entrega individual")
    public ResponseEntity<List<GanhosPorEntregaDTO>> listarGanhosPorEntrega(
            @PathVariable Long id,
            @RequestParam(defaultValue = "MES") Periodo periodo) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado"));
        SecurityUtils.validateEntregadorOwnership(entregador);
        
        List<GanhosPorEntregaDTO> response = ganhosService.listarGanhosPorEntrega(id, periodo);
        return ResponseEntity.ok(response);
    }
}

