package com.siseg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnderecoRequestDTO {
    
    @NotBlank(message = "Logradouro é obrigatório")
    @Size(max = 200, message = "Logradouro deve ter no máximo 200 caracteres")
    private String logradouro;
    
    @NotBlank(message = "Número é obrigatório")
    @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
    private String numero;
    
    @Size(max = 50, message = "Complemento deve ter no máximo 50 caracteres")
    private String complemento;
    
    @NotBlank(message = "Bairro é obrigatório")
    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    private String bairro;
    
    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    private String cidade;
    
    @NotBlank(message = "Estado é obrigatório")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ser sigla de 2 letras maiúsculas (ex: SP)")
    private String estado;
    
    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP deve ter formato 12345-678 ou 12345678")
    private String cep;
    
    private Boolean principal;
}

