package com.siseg.dto.ganhos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioDistribuicaoDTO {
    private BigDecimal volumeTotal;
    private BigDecimal distribuicaoRestaurantes;
    private BigDecimal distribuicaoEntregadores;
    private BigDecimal distribuicaoPlataforma;
    private String periodo;
    private String tendencia;
}

