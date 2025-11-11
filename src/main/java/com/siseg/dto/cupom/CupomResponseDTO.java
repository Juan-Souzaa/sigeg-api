package com.siseg.dto.cupom;

import com.siseg.model.enumerations.TipoDesconto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CupomResponseDTO {
    private Long id;
    private String codigo;
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;
    private BigDecimal valorMinimo;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Boolean ativo;
    private Integer usosMaximos;
    private Integer usosAtuais;
    private Instant criadoEm;
}

