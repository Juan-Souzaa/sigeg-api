package com.siseg.dto.entregador;

import com.siseg.model.enumerations.TipoVeiculo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EntregadorUpdateDTO {
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String nome;

    @Size(min = 10, max = 15, message = "Telefone deve ter entre 10 e 15 caracteres")
    private String telefone;

    @Email(message = "Email deve ter formato válido")
    private String email;

    private TipoVeiculo tipoVeiculo;

    @Size(min = 7, max = 8, message = "Placa do veículo deve ter entre 7 e 8 caracteres")
    private String placaVeiculo;

    private BigDecimal latitude;
    private BigDecimal longitude;
}

