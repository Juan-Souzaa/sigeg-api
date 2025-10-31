package com.siseg.dto.pedido;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PedidoItemResponseDTO {
    private Long id;
    private Long pratoId;
    private String pratoNome;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
}
