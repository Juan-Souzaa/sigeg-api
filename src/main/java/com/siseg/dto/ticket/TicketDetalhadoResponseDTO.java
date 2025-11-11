package com.siseg.dto.ticket;

import lombok.Data;

import java.util.List;

@Data
public class TicketDetalhadoResponseDTO {
    private TicketResponseDTO ticket;
    private List<TicketComentarioResponseDTO> comentarios;
}

