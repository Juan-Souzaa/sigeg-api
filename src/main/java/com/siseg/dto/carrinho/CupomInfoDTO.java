package com.siseg.dto.carrinho;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CupomInfoDTO {
    private Long id;
    private String codigo;
    private String tipoDesconto;
    private BigDecimal valorDesconto;
}

