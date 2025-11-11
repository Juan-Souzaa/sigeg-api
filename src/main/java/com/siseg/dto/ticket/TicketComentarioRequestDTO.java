package com.siseg.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketComentarioRequestDTO {
    @NotBlank(message = "Comentario e obrigatorio")
    private String comentario;

    private Boolean interno = false;
}

