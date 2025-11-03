package com.siseg.dto.pedido;

import com.siseg.dto.entregador.EntregadorSimplesDTO;
import com.siseg.dto.rastreamento.RastreamentoDTO;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPedido;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class PedidoResponseDTO {
    private Long id;
    private Long clienteId;
    private Long restauranteId;
    private StatusPedido status;
    private MetodoPagamento metodoPagamento;
    private BigDecimal troco;
    private String observacoes;
    private String enderecoEntrega;
    private BigDecimal subtotal;
    private BigDecimal taxaEntrega;
    private BigDecimal total;
    private List<PedidoItemResponseDTO> itens;
    private EntregadorSimplesDTO entregador;
    private Instant tempoEstimadoEntrega;
    private Instant criadoEm;
    private RastreamentoDTO rastreamento;
}
