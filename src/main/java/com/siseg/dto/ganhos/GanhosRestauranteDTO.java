package com.siseg.dto.ganhos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GanhosRestauranteDTO {
    private BigDecimal valorBruto;
    private BigDecimal taxaPlataforma;
    private BigDecimal percentualTaxa;
    private BigDecimal valorLiquido;
    private Long totalPedidos;
    private String periodo;
}

