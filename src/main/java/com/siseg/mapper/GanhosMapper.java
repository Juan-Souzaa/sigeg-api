package com.siseg.mapper;

import com.siseg.dto.ganhos.GanhosEntregadorDTO;
import com.siseg.dto.ganhos.GanhosPorEntregaDTO;
import com.siseg.dto.ganhos.GanhosRestauranteDTO;
import com.siseg.dto.ganhos.RelatorioDistribuicaoDTO;
import com.siseg.model.Pedido;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class GanhosMapper {

    private final ModelMapper modelMapper;

    public GanhosMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public GanhosRestauranteDTO toGanhosRestauranteDTO(BigDecimal valorBruto, BigDecimal taxaPlataforma,
                                                        BigDecimal percentualTaxa, BigDecimal valorLiquido,
                                                        Long totalPedidos, String periodo) {
        GanhosRestauranteDTO dto = new GanhosRestauranteDTO();
        dto.setValorBruto(valorBruto);
        dto.setTaxaPlataforma(taxaPlataforma);
        dto.setPercentualTaxa(percentualTaxa);
        dto.setValorLiquido(valorLiquido);
        dto.setTotalPedidos(totalPedidos);
        dto.setPeriodo(periodo);
        return dto;
    }

    public GanhosEntregadorDTO toGanhosEntregadorDTO(BigDecimal valorBruto, BigDecimal taxaPlataforma,
                                                      BigDecimal percentualTaxa, BigDecimal valorLiquido,
                                                      Long totalEntregas, String periodo) {
        GanhosEntregadorDTO dto = new GanhosEntregadorDTO();
        dto.setValorBruto(valorBruto);
        dto.setTaxaPlataforma(taxaPlataforma);
        dto.setPercentualTaxa(percentualTaxa);
        dto.setValorLiquido(valorLiquido);
        dto.setTotalEntregas(totalEntregas);
        dto.setPeriodo(periodo);
        return dto;
    }

    public GanhosPorEntregaDTO toGanhosPorEntregaDTO(Pedido pedido) {
       
        GanhosPorEntregaDTO dto = new GanhosPorEntregaDTO();
        dto.setPedidoId(pedido.getId());
        dto.setTaxaEntrega(pedido.getTaxaEntrega());
        dto.setTaxaPlataforma(pedido.getTaxaPlataformaEntregador());
        dto.setValorLiquido(pedido.getValorLiquidoEntregador());
        dto.setDataEntrega(pedido.getCriadoEm());
        return dto;
    }

    public List<GanhosPorEntregaDTO> toGanhosPorEntregaDTOList(List<Pedido> pedidos) {
        return pedidos.stream()
                .map(this::toGanhosPorEntregaDTO)
                .toList();
    }

    public RelatorioDistribuicaoDTO toRelatorioDistribuicaoDTO(BigDecimal volumeTotal,
                                                                BigDecimal distribuicaoRestaurantes,
                                                                BigDecimal distribuicaoEntregadores,
                                                                BigDecimal distribuicaoPlataforma,
                                                                String periodo, String tendencia) {
        RelatorioDistribuicaoDTO dto = new RelatorioDistribuicaoDTO();
        dto.setVolumeTotal(volumeTotal);
        dto.setDistribuicaoRestaurantes(distribuicaoRestaurantes);
        dto.setDistribuicaoEntregadores(distribuicaoEntregadores);
        dto.setDistribuicaoPlataforma(distribuicaoPlataforma);
        dto.setPeriodo(periodo);
        dto.setTendencia(tendencia);
        return dto;
    }
}

