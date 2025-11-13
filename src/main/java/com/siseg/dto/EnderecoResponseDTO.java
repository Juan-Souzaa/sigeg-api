package com.siseg.dto;

import com.siseg.model.enumerations.TipoEndereco;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class EnderecoResponseDTO {
    
    private Long id;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private TipoEndereco tipo;
    private Boolean principal;
    private Instant criadoEm;
}

