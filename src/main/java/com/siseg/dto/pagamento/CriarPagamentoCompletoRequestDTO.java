package com.siseg.dto.pagamento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CriarPagamentoCompletoRequestDTO {
    @NotNull(message = "Dados do pagamento são obrigatórios")
    @Valid
    private CriarPagamentoRequestDTO pagamento;
    
    @NotNull(message = "Dados do cliente são obrigatórios")
    @Valid
    private ClienteInfoDTO cliente;
}


