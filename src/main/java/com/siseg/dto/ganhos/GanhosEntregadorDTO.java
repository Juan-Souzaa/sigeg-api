package com.siseg.dto.ganhos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GanhosEntregadorDTO {
    private BigDecimal valorBruto;
    private BigDecimal taxaPlataforma;
    private BigDecimal percentualTaxa;
    private BigDecimal valorLiquido;
    private Long totalEntregas;
    private String periodo;
}

