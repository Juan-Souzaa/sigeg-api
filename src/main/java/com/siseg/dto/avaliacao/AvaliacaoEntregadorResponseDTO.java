package com.siseg.dto.avaliacao;

import lombok.Data;

import java.time.Instant;

@Data
public class AvaliacaoEntregadorResponseDTO {
    private Long id;
    private Long pedidoId;
    private Long clienteId;
    private Long entregadorId;
    private Integer notaEntregador;
    private String comentarioEntregador;
    private Instant criadoEm;
    private Instant atualizadoEm;
}

