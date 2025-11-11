package com.siseg.model;

import com.siseg.model.enumerations.PrioridadeTicket;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.model.enumerations.TipoTicket;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTicket tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTicket status = StatusTicket.ABERTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadeTicket prioridade = PrioridadeTicket.MEDIA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private User criadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atribuido_a_id")
    private User atribuidoA;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column
    private Instant atualizadoEm;

    @Column
    private Instant resolvidoEm;
}

