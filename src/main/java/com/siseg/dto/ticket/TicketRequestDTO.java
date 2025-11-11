package com.siseg.dto.ticket;

import com.siseg.model.enumerations.PrioridadeTicket;
import com.siseg.model.enumerations.TipoTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketRequestDTO {
    @NotBlank(message = "Titulo e obrigatorio")
    private String titulo;

    @NotBlank(message = "Descricao e obrigatoria")
    private String descricao;

    @NotNull(message = "Tipo do ticket e obrigatorio")
    private TipoTicket tipo;

    @NotNull(message = "Prioridade e obrigatoria")
    private PrioridadeTicket prioridade;
}

