package com.siseg.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "avaliacoes", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "pedido_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Avaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurante_id", nullable = false)
    private Restaurante restaurante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregador_id")
    private Entregador entregador;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer notaRestaurante;

    @Min(1)
    @Max(5)
    @Column
    private Integer notaEntregador;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer notaPedido;

    @Column(columnDefinition = "TEXT")
    private String comentarioRestaurante;

    @Column(columnDefinition = "TEXT")
    private String comentarioEntregador;

    @Column(columnDefinition = "TEXT")
    private String comentarioPedido;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column
    private Instant atualizadoEm;
}

