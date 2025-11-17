package com.siseg.dto.restaurante;

import com.siseg.dto.EnderecoRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RestauranteRequestDTO {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    
    @Valid
    @NotNull(message = "Endereço é obrigatório")
    private EnderecoRequestDTO endereco;
    
    @NotBlank(message = "Telefone é obrigatório")
    private String telefone;
    
    @Email(message = "Email deve ser válido")
    @NotBlank(message = "Email é obrigatório")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 20, message = "Senha deve ter entre 6 e 20 caracteres")
    private String password;

    @DecimalMin(value = "0.1", message = "Raio de entrega deve ser no mínimo 0.1 km")
    private BigDecimal raioEntregaKm;
}
