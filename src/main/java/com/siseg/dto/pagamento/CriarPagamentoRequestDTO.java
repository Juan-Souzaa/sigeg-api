package com.siseg.dto.pagamento;

import com.siseg.model.enumerations.MetodoPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CriarPagamentoRequestDTO {
    @NotNull(message = "ID do pedido é obrigatório")
    @Positive(message = "ID do pedido deve ser positivo")
    private Long pedidoId;
    
    @NotNull(message = "Método de pagamento é obrigatório")
    private MetodoPagamento metodoPagamento;
    
    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private BigDecimal valor;
    
    private BigDecimal troco;
    
    private CartaoCreditoRequestDTO cartaoCredito;
}


