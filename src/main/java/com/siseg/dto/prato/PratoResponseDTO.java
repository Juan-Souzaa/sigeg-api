package com.siseg.dto.prato;

import com.siseg.model.enumerations.CategoriaMenu;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PratoResponseDTO {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private CategoriaMenu categoria;
    private Boolean disponivel;
    private String fotoUrl;
    private Long restauranteId;
    private Instant criadoEm;
}
