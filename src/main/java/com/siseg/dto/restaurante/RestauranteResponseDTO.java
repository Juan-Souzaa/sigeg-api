package com.siseg.dto.restaurante;

import com.siseg.model.enumerations.StatusRestaurante;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class RestauranteResponseDTO {
    private Long id;
    private String nome;
    private String endereco;
    private String telefone;
    private String email;
    private StatusRestaurante status;
    private Boolean ativo;
    private BigDecimal raioEntregaKm;
    private String fotoUrl;
    private Instant criadoEm;
}
