package com.siseg.model;

import com.siseg.model.enumerations.StatusRestaurante;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "restaurantes")
@Getter
@Setter
@NoArgsConstructor
public class Restaurante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String endereco;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRestaurante status = StatusRestaurante.PENDING_APPROVAL;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
}
