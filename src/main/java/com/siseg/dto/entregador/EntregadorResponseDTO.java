package com.siseg.dto.entregador;

import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.model.enumerations.DisponibilidadeEntregador;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class EntregadorResponseDTO {
    private Long id;
    private Long userId;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;
    private String fotoCnhUrl;
    private TipoVeiculo tipoVeiculo;
    private String placaVeiculo;
    private StatusEntregador status;
    private DisponibilidadeEntregador disponibilidade;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Instant criadoEm;
    private Instant atualizadoEm;
}

