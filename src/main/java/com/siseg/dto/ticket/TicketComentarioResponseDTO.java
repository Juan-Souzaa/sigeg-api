package com.siseg.dto.ticket;

import lombok.Data;

import java.time.Instant;

@Data
public class TicketComentarioResponseDTO {
    private Long id;
    private Long ticketId;
    private Long autorId;
    private String autorNome;
    private String comentario;
    private Boolean interno;
    private Instant criadoEm;
}

