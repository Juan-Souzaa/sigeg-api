package com.siseg.dto.restaurante;

import com.siseg.dto.EnderecoRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
}
