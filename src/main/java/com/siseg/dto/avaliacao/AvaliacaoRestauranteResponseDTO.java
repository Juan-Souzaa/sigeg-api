package com.siseg.dto.avaliacao;

import lombok.Data;

import java.time.Instant;

@Data
public class AvaliacaoRestauranteResponseDTO {
    private Long id;
    private Long pedidoId;
    private Long clienteId;
    private Long restauranteId;
    private Integer notaRestaurante;
    private Integer notaPedido;
    private String comentarioRestaurante;
    private String comentarioPedido;
    private Instant criadoEm;
    private Instant atualizadoEm;
}

