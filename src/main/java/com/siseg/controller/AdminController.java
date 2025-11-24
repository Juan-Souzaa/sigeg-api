package com.siseg.controller;

import com.siseg.dto.admin.AdminRequestDTO;
import com.siseg.dto.admin.AdminResponseDTO;
import com.siseg.dto.configuracao.ConfiguracaoTaxaRequestDTO;
import com.siseg.dto.configuracao.ConfiguracaoTaxaResponseDTO;
import com.siseg.dto.ganhos.RelatorioDistribuicaoDTO;
import com.siseg.dto.ganhos.RelatorioCompletoDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.model.enumerations.Periodo;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoTaxa;
import com.siseg.repository.PedidoRepository;
import com.siseg.service.AdminService;
import com.siseg.service.ConfiguracaoTaxaService;
import com.siseg.service.GanhosService;
import com.siseg.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Operações administrativas")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PedidoService pedidoService;
    private final GanhosService ganhosService;
    private final ConfiguracaoTaxaService configuracaoTaxaService;
    private final PedidoRepository pedidoRepository;
    private final AdminService adminService;

    public AdminController(PedidoService pedidoService, GanhosService ganhosService,
                          ConfiguracaoTaxaService configuracaoTaxaService, PedidoRepository pedidoRepository,
                          AdminService adminService) {
        this.pedidoService = pedidoService;
        this.ganhosService = ganhosService;
        this.configuracaoTaxaService = configuracaoTaxaService;
        this.pedidoRepository = pedidoRepository;
        this.adminService = adminService;
    }

    @GetMapping("/pedidos/andamento")
    @Operation(summary = "Listar pedidos em andamento")
    public ResponseEntity<Page<PedidoResponseDTO>> listarPedidosAndamento(Pageable pageable) {
        List<PedidoResponseDTO> pedidos = pedidoRepository.findByStatus(StatusPedido.PREPARING).stream()
                .map(p -> pedidoService.buscarPorId(p.getId()))
                .toList();
        Page<PedidoResponseDTO> page = new PageImpl<>(pedidos, pageable, pedidos.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/relatorios/vendas")
    @Operation(summary = "Relatório básico de vendas")
    public ResponseEntity<Page<PedidoResponseDTO>> relatorioVendas(Pageable pageable) {
        List<PedidoResponseDTO> pedidos = pedidoRepository.findByStatus(StatusPedido.DELIVERED).stream()
                .map(p -> pedidoService.buscarPorId(p.getId()))
                .toList();
        Page<PedidoResponseDTO> page = new PageImpl<>(pedidos, pageable, pedidos.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/relatorios/distribuicao")
    @Operation(summary = "Relatório de distribuição de valores")
    public ResponseEntity<RelatorioDistribuicaoDTO> relatorioDistribuicao(
            @RequestParam(defaultValue = "MES") Periodo periodo) {
        RelatorioDistribuicaoDTO response = ganhosService.gerarRelatorioDistribuicao(periodo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/relatorios/completo")
    @Operation(summary = "Relatório completo com todas as estatísticas")
    public ResponseEntity<RelatorioCompletoDTO> relatorioCompleto(
            @RequestParam(defaultValue = "MES") Periodo periodo) {
        RelatorioCompletoDTO response = ganhosService.gerarRelatorioCompleto(periodo);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/configuracoes/taxas")
    @Operation(summary = "Criar configuração de taxa")
    public ResponseEntity<ConfiguracaoTaxaResponseDTO> criarConfiguracaoTaxa(
            @Valid @RequestBody ConfiguracaoTaxaRequestDTO dto) {
        ConfiguracaoTaxaResponseDTO response = configuracaoTaxaService.criarConfiguracaoTaxa(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/configuracoes/taxas")
    @Operation(summary = "Listar histórico de taxas")
    public ResponseEntity<List<ConfiguracaoTaxaResponseDTO>> listarHistoricoTaxas(
            @RequestParam TipoTaxa tipoTaxa) {
        List<ConfiguracaoTaxaResponseDTO> response = configuracaoTaxaService.listarHistoricoTaxas(tipoTaxa);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admins")
    @Operation(summary = "Criar novo administrador")
    public ResponseEntity<AdminResponseDTO> criarAdmin(@Valid @RequestBody AdminRequestDTO dto) {
        AdminResponseDTO response = adminService.criarAdmin(dto);
        return ResponseEntity.ok(response);
    }
}

