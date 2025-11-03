package com.siseg.dto.avaliacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvaliacaoRequestDTO {
    
    @NotNull(message = "Nota do restaurante é obrigatória")
    @Min(value = 1, message = "Nota deve ser entre 1 e 5")
    @Max(value = 5, message = "Nota deve ser entre 1 e 5")
    private Integer notaRestaurante;
    
    @Min(value = 1, message = "Nota deve ser entre 1 e 5")
    @Max(value = 5, message = "Nota deve ser entre 1 e 5")
    private Integer notaEntregador;
    
    @NotNull(message = "Nota do pedido é obrigatória")
    @Min(value = 1, message = "Nota deve ser entre 1 e 5")
    @Max(value = 5, message = "Nota deve ser entre 1 e 5")
    private Integer notaPedido;
    
    private String comentarioRestaurante;
    
    private String comentarioEntregador;
    
    private String comentarioPedido;
}

