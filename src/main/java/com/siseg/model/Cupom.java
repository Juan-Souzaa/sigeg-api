package com.siseg.model;

import com.siseg.model.enumerations.TipoDesconto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cupons")
@Getter
@Setter
@NoArgsConstructor
public class Cupom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDesconto tipoDesconto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDesconto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMinimo;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = false)
    private LocalDate dataFim;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(nullable = false)
    private Integer usosMaximos;

    @Column(nullable = false)
    private Integer usosAtuais = 0;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
}

