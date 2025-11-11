package com.siseg.service;

import com.siseg.dto.ganhos.GanhosEntregadorDTO;
import com.siseg.dto.ganhos.GanhosPorEntregaDTO;
import com.siseg.dto.ganhos.GanhosRestauranteDTO;
import com.siseg.dto.ganhos.RelatorioDistribuicaoDTO;
import com.siseg.mapper.GanhosMapper;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.Periodo;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class GanhosService {

    private final PedidoRepository pedidoRepository;
    private final GanhosMapper ganhosMapper;

    public GanhosService(PedidoRepository pedidoRepository, GanhosMapper ganhosMapper) {
        this.pedidoRepository = pedidoRepository;
        this.ganhosMapper = ganhosMapper;
    }

    @Transactional(readOnly = true)
    public GanhosRestauranteDTO calcularGanhosRestaurante(Long restauranteId, Periodo periodo) {
        var periodoDatas = obterPeriodoDatas(periodo);
        List<Pedido> pedidos = buscarPedidosEntreguesRestaurante(restauranteId, periodoDatas.inicio, periodoDatas.fim);
        
        BigDecimal valorBruto = calcularValorBrutoRestaurante(pedidos);
        BigDecimal taxaPlataforma = somarTaxaPlataformaRestaurante(pedidos);
        BigDecimal valorLiquido = somarValorLiquidoRestaurante(pedidos);
        BigDecimal percentualTaxa = calcularPercentualTaxaMedio(valorBruto, taxaPlataforma);
        
        return ganhosMapper.toGanhosRestauranteDTO(
                valorBruto, taxaPlataforma, percentualTaxa, valorLiquido,
                (long) pedidos.size(), periodo.name()
        );
    }

    @Transactional(readOnly = true)
    public GanhosEntregadorDTO calcularGanhosEntregador(Long entregadorId, Periodo periodo) {
        var periodoDatas = obterPeriodoDatas(periodo);
        List<Pedido> pedidos = buscarPedidosEntreguesEntregador(entregadorId, periodoDatas.inicio, periodoDatas.fim);
        
        BigDecimal valorBruto = calcularValorBrutoEntregador(pedidos);
        BigDecimal taxaPlataforma = somarTaxaPlataformaEntregador(pedidos);
        BigDecimal valorLiquido = somarValorLiquidoEntregador(pedidos);
        BigDecimal percentualTaxa = calcularPercentualTaxaMedio(valorBruto, taxaPlataforma);
        
        return ganhosMapper.toGanhosEntregadorDTO(
                valorBruto, taxaPlataforma, percentualTaxa, valorLiquido,
                (long) pedidos.size(), periodo.name()
        );
    }

    @Transactional(readOnly = true)
    public List<GanhosPorEntregaDTO> listarGanhosPorEntrega(Long entregadorId, Periodo periodo) {
        var periodoDatas = obterPeriodoDatas(periodo);
        List<Pedido> pedidos = buscarPedidosEntreguesEntregador(entregadorId, periodoDatas.inicio, periodoDatas.fim);
        return ganhosMapper.toGanhosPorEntregaDTOList(pedidos);
    }

    @Transactional(readOnly = true)
    public RelatorioDistribuicaoDTO gerarRelatorioDistribuicao(Periodo periodo) {
        var periodoDatas = obterPeriodoDatas(periodo);
        List<Pedido> pedidos = buscarPedidosEntregues(periodoDatas.inicio, periodoDatas.fim);
        
        BigDecimal volumeTotal = calcularVolumeTotal(pedidos);
        BigDecimal distribuicaoRestaurantes = calcularDistribuicaoRestaurantes(pedidos);
        BigDecimal distribuicaoEntregadores = calcularDistribuicaoEntregadores(pedidos);
        BigDecimal distribuicaoPlataforma = volumeTotal.subtract(distribuicaoRestaurantes).subtract(distribuicaoEntregadores);
        
        String tendencia = calcularTendencia(pedidos, periodo);
        
        return ganhosMapper.toRelatorioDistribuicaoDTO(
                volumeTotal, distribuicaoRestaurantes, distribuicaoEntregadores,
                distribuicaoPlataforma, periodo.name(), tendencia
        );
    }

    private List<Pedido> buscarPedidosEntreguesRestaurante(Long restauranteId, Instant inicio, Instant fim) {
        return pedidoRepository.findByStatusAndRestauranteIdAndCriadoEmBetween(
                StatusPedido.DELIVERED, restauranteId, inicio, fim);
    }

    private List<Pedido> buscarPedidosEntreguesEntregador(Long entregadorId, Instant inicio, Instant fim) {
        return pedidoRepository.findByStatusAndEntregadorIdAndCriadoEmBetween(
                StatusPedido.DELIVERED, entregadorId, inicio, fim);
    }

    private List<Pedido> buscarPedidosEntregues(Instant inicio, Instant fim) {
        return pedidoRepository.findByStatus(StatusPedido.DELIVERED).stream()
                .filter(p -> p.getCriadoEm().isAfter(inicio) && p.getCriadoEm().isBefore(fim))
                .toList();
    }

    private BigDecimal calcularValorBrutoRestaurante(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getSubtotal() != null ? p.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularValorBrutoEntregador(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getTaxaEntrega() != null ? p.getTaxaEntrega() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarTaxaPlataformaRestaurante(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getTaxaPlataformaRestaurante() != null ? p.getTaxaPlataformaRestaurante() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarTaxaPlataformaEntregador(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getTaxaPlataformaEntregador() != null ? p.getTaxaPlataformaEntregador() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarValorLiquidoRestaurante(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getValorLiquidoRestaurante() != null ? p.getValorLiquidoRestaurante() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarValorLiquidoEntregador(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getValorLiquidoEntregador() != null ? p.getValorLiquidoEntregador() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPercentualTaxaMedio(BigDecimal valorBruto, BigDecimal taxaPlataforma) {
        if (valorBruto == null || valorBruto.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (taxaPlataforma == null) {
            return BigDecimal.ZERO;
        }
        return taxaPlataforma.multiply(new BigDecimal("100"))
                .divide(valorBruto, 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calcularVolumeTotal(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDistribuicaoRestaurantes(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getValorLiquidoRestaurante() != null ? p.getValorLiquidoRestaurante() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularDistribuicaoEntregadores(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(p -> p.getValorLiquidoEntregador() != null ? p.getValorLiquidoEntregador() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String calcularTendencia(List<Pedido> pedidos, Periodo periodo) {
        if (pedidos.size() < 2) {
            return "INSUFICIENTE";
        }
        return "ESTAVEL";
    }

    private PeriodoDatas obterPeriodoDatas(Periodo periodo) {
        Instant fim = Instant.now();
        Instant inicio;
        
        switch (periodo) {
            case HOJE:
                inicio = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
                break;
            case SEMANA:
                inicio = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
                break;
            case MES:
                inicio = LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
                break;
            default:
                inicio = LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        
        return new PeriodoDatas(inicio, fim);
    }

    private record PeriodoDatas(Instant inicio, Instant fim) {}
}

