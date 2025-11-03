package com.siseg.dto.avaliacao;

import lombok.Data;

import java.time.Instant;

@Data
public class AvaliacaoResponseDTO {
    private Long id;
    private Long pedidoId;
    private Long clienteId;
    private Long restauranteId;
    private Long entregadorId;
    private Integer notaRestaurante;
    private Integer notaEntregador;
    private Integer notaPedido;
    private String comentarioRestaurante;
    private String comentarioEntregador;
    private String comentarioPedido;
    private Instant criadoEm;
    private Instant atualizadoEm;
}

