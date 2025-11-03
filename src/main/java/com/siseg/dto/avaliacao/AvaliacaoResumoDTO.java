package com.siseg.dto.avaliacao;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AvaliacaoResumoDTO {
    private BigDecimal mediaNotaRestaurante;
    private Long totalAvaliacoesRestaurante;
}

