package com.siseg.dto.ganhos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GanhosPorEntregaDTO {
    private Long pedidoId;
    private BigDecimal taxaEntrega;
    private BigDecimal taxaPlataforma;
    private BigDecimal valorLiquido;
    private Instant dataEntrega;
}

