package com.siseg.dto.cupom;

import com.siseg.model.enumerations.TipoDesconto;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CupomRequestDTO {
    @NotBlank(message = "Código do cupom é obrigatório")
    @Size(min = 3, max = 50, message = "Código deve ter entre 3 e 50 caracteres")
    private String codigo;

    @NotNull(message = "Tipo de desconto é obrigatório")
    private TipoDesconto tipoDesconto;

    @NotNull(message = "Valor do desconto é obrigatório")
    @DecimalMin(value = "0.0", message = "Valor do desconto deve ser maior ou igual a zero")
    private BigDecimal valorDesconto;

    @NotNull(message = "Valor mínimo é obrigatório")
    @DecimalMin(value = "0.0", message = "Valor mínimo deve ser maior ou igual a zero")
    private BigDecimal valorMinimo;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private LocalDate dataFim;

    @NotNull(message = "Usos máximos é obrigatório")
    @Min(value = 1, message = "Usos máximos deve ser maior que zero")
    private Integer usosMaximos;
}

