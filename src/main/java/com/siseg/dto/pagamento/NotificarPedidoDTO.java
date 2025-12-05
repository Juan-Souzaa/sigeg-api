package com.siseg.dto.pagamento;

import com.siseg.model.enumerations.StatusPagamento;
import lombok.Data;

@Data
public class NotificarPedidoDTO {
    private Long pedidoId;
    private StatusPagamento statusPagamento;
    private String asaasPaymentId;
}


