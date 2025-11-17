package com.siseg.dto.restaurante;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RestauranteBuscaDTO {
    private Long id;
    private String nome;
    private String endereco;
    private String telefone;
    private BigDecimal distanciaKm;
    private Integer tempoEstimadoMinutos;
    private BigDecimal raioEntregaKm;
    private BigDecimal mediaAvaliacao;
    private Long totalAvaliacoes;
}
