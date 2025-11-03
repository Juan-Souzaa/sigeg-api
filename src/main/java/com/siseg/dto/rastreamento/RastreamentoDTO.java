package com.siseg.dto.rastreamento;

import com.siseg.model.enumerations.StatusPedido;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RastreamentoDTO {
    private BigDecimal posicaoAtualLat;
    private BigDecimal posicaoAtualLon;
    private BigDecimal posicaoDestinoLat;
    private BigDecimal posicaoDestinoLon;
    private BigDecimal distanciaRestanteKm;
    private Integer tempoEstimadoMinutos;
    private StatusPedido statusEntrega;
    private Boolean proximoAoDestino;
}

