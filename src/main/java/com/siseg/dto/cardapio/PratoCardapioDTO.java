package com.siseg.dto.cardapio;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PratoCardapioDTO {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private Boolean disponivel;
    private String fotoUrl;
}
