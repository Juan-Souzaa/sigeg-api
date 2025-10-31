package com.siseg.dto.restaurante;

import com.siseg.model.enumerations.StatusRestaurante;
import lombok.Data;

import java.time.Instant;

@Data
public class RestauranteResponseDTO {
    private Long id;
    private String nome;
    private String endereco;
    private String telefone;
    private String email;
    private StatusRestaurante status;
    private Instant criadoEm;
}
