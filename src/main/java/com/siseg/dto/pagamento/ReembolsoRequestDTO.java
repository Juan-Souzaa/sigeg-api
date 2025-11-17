package com.siseg.dto.pagamento;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReembolsoRequestDTO {
    @NotBlank(message = "Motivo do reembolso é obrigatório")
    private String motivo;
}

