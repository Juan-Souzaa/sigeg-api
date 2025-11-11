package com.siseg.dto.ticket;

import com.siseg.model.enumerations.PrioridadeTicket;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.model.enumerations.TipoTicket;
import lombok.Data;

import java.time.Instant;

@Data
public class TicketResponseDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private TipoTicket tipo;
    private StatusTicket status;
    private PrioridadeTicket prioridade;
    private Long criadoPorId;
    private String criadoPorNome;
    private Long atribuidoAId;
    private String atribuidoANome;
    private Instant criadoEm;
    private Instant atualizadoEm;
    private Instant resolvidoEm;
}

