package com.siseg.dto.pedido;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PedidoItemRequestDTO {
    @NotNull(message = "ID do prato é obrigatório")
    private Long pratoId;
    
    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser positiva")
    private Integer quantidade;
}
