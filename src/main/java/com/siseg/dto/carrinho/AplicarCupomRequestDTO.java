package com.siseg.dto.carrinho;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AplicarCupomRequestDTO {
    @NotBlank(message = "Código do cupom é obrigatório")
    private String codigo;
}

