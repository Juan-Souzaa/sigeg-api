package com.siseg.dto.configuracao;

import com.siseg.model.enumerations.TipoTaxa;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoTaxaResponseDTO {
    private Long id;
    private TipoTaxa tipoTaxa;
    private BigDecimal percentual;
    private Boolean ativo;
    private Instant criadoEm;
    private Instant atualizadoEm;
}

