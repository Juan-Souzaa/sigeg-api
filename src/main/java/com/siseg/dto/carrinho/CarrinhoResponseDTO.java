package com.siseg.dto.carrinho;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarrinhoResponseDTO {
    private Long id;
    private Long clienteId;
    private List<CarrinhoItemResponseDTO> itens;
    private CupomInfoDTO cupom;
    private BigDecimal subtotal;
    private BigDecimal desconto;
    private BigDecimal total;
    private Instant criadoEm;
    private Instant atualizadoEm;
}

