package com.siseg.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnderecoCepResponseDTO {
    private String logradouro;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
}

