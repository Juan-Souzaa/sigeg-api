package com.siseg.dto.avaliacao;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AvaliacaoResumoEntregadorDTO {
    private BigDecimal mediaNotaEntregador;
    private Long totalAvaliacoesEntregador;
}

