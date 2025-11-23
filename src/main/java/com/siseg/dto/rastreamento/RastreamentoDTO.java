package com.siseg.dto.rastreamento;

import com.siseg.dto.geocoding.Coordinates;
import com.siseg.model.enumerations.StatusPedido;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RastreamentoDTO {
    private BigDecimal posicaoAtualLat;
    private BigDecimal posicaoAtualLon;
    private BigDecimal posicaoDestinoLat;
    private BigDecimal posicaoDestinoLon;
    private BigDecimal posicaoRestauranteLat;
    private BigDecimal posicaoRestauranteLon;
    private BigDecimal distanciaRestanteKm;
    private Integer tempoEstimadoMinutos;
    private StatusPedido statusEntrega;
    private Boolean proximoAoDestino;
    private List<Coordinates> waypoints;
}

