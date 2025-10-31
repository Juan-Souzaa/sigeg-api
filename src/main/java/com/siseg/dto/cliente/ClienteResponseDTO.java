package com.siseg.dto.cliente;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ClienteResponseDTO {
    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private String endereco;
    private Instant criadoEm;
}
