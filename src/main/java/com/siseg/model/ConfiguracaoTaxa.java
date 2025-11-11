package com.siseg.model;

import com.siseg.model.enumerations.TipoTaxa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "configuracoes_taxa")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracaoTaxa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTaxa tipoTaxa;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentual;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private Instant atualizadoEm = Instant.now();

    @PreUpdate
    private void preUpdate() {
        this.atualizadoEm = Instant.now();
    }
}

