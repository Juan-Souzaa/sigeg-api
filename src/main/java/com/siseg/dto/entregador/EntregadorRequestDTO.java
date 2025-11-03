package com.siseg.dto.entregador;

import com.siseg.model.enumerations.TipoVeiculo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EntregadorRequestDTO {
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 14, message = "CPF deve ter entre 11 e 14 caracteres")
    private String cpf;

    @NotBlank(message = "Telefone é obrigatório")
    @Size(min = 10, max = 15, message = "Telefone deve ter entre 10 e 15 caracteres")
    private String telefone;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    private String email;

    private String fotoCnhUrl;

    @NotNull(message = "Tipo de veículo é obrigatório")
    private TipoVeiculo tipoVeiculo;

    @NotBlank(message = "Placa do veículo é obrigatória")
    @Size(min = 7, max = 8, message = "Placa do veículo deve ter entre 7 e 8 caracteres")
    private String placaVeiculo;

    @NotNull(message = "Latitude é obrigatória")
    private BigDecimal latitude;

    @NotNull(message = "Longitude é obrigatória")
    private BigDecimal longitude;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 20, message = "Senha deve ter entre 6 e 20 caracteres")
    private String password;
}

