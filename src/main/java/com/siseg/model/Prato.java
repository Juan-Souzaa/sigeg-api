package com.siseg.model;

import com.siseg.model.enumerations.CategoriaMenu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pratos")
@Getter
@Setter
@NoArgsConstructor
public class Prato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaMenu categoria;

    @Column(nullable = false)
    private Boolean disponivel = true;

    private String fotoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurante_id", nullable = false)
    private Restaurante restaurante;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
}
