package com.siseg.dto.pedido;

import com.siseg.model.enumerations.MetodoPagamento;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PedidoRequestDTO {
    @NotNull(message = "ID do restaurante é obrigatório")
    private Long restauranteId;
    
    @NotEmpty(message = "Itens do pedido são obrigatórios")
    private List<PedidoItemRequestDTO> itens;
    
    @NotNull(message = "Método de pagamento é obrigatório")
    private MetodoPagamento metodoPagamento;
    
    @DecimalMin(value = "0.0", message = "Troco deve ser positivo")
    private BigDecimal troco;
    
    private String observacoes;
    
    private Long enderecoId; // Opcional: se não fornecido, usa endereço principal do cliente
    
    private Long carrinhoId;
}
