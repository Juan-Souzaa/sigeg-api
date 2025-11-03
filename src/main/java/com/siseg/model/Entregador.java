package com.siseg.model;

import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.TipoVeiculo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "entregadores")
@Getter
@Setter
@NoArgsConstructor
public class Entregador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String fotoCnhUrl; // URL ou path para a foto da CNH

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVeiculo tipoVeiculo;

    @Column(nullable = false)
    private String placaVeiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEntregador status = StatusEntregador.PENDING_APPROVAL;

    @Column(precision = 10, scale = 8) // Latitude com precisão para geolocalização
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8) // Longitude com precisão para geolocalização
    private BigDecimal longitude;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    private Instant atualizadoEm;
}

