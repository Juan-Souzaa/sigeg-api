package com.siseg.dto.restaurante;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RestauranteRequestDTO {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    
    @NotBlank(message = "Endereço é obrigatório")
    private String endereco;
    
    @NotBlank(message = "Telefone é obrigatório")
    private String telefone;
    
    @Email(message = "Email deve ser válido")
    @NotBlank(message = "Email é obrigatório")
    private String email;
}
