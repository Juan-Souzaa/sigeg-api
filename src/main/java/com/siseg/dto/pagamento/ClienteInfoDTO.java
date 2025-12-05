package com.siseg.dto.pagamento;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClienteInfoDTO {
    @NotNull(message = "ID do cliente é obrigatório")
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    private String email;
    
    @NotBlank(message = "Telefone é obrigatório")
    private String telefone;
    
    private String cpfCnpj;
}


