package com.siseg.dto.configuracao;

import com.siseg.model.enumerations.TipoTaxa;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfiguracaoTaxaRequestDTO {
    @NotNull(message = "Tipo de taxa é obrigatório")
    private TipoTaxa tipoTaxa;

    @NotNull(message = "Percentual é obrigatório")
    @DecimalMin(value = "0.0", message = "Percentual deve ser maior ou igual a 0")
    @DecimalMax(value = "100.0", message = "Percentual deve ser menor ou igual a 100")
    private BigDecimal percentual;
}

