package com.siseg.dto.restaurante;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AtualizarRaioEntregaDTO {
    @NotNull(message = "Raio de entrega é obrigatório")
    @DecimalMin(value = "0.1", message = "Raio de entrega deve ser no mínimo 0.1 km")
    private BigDecimal raioEntregaKm;
}

