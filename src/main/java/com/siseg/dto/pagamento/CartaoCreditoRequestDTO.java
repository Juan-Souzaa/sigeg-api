package com.siseg.dto.pagamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CartaoCreditoRequestDTO {
    
    @NotBlank(message = "Número do cartão é obrigatório")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Número do cartão deve ter entre 13 e 19 dígitos")
    private String numero;
    
    @NotBlank(message = "Nome do titular é obrigatório")
    private String nomeTitular;
    
    @NotBlank(message = "Validade é obrigatória")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "Validade deve estar no formato MM/YY")
    private String validade;
    
    @NotBlank(message = "CVV é obrigatório")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV deve ter 3 ou 4 dígitos")
    private String cvv;
}

