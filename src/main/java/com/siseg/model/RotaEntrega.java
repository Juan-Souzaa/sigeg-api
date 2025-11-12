package com.siseg.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rota_entrega")
@Getter
@Setter
@NoArgsConstructor
public class RotaEntrega {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;
    
    @Column(name = "waypoints", columnDefinition = "TEXT")
    private String waypointsJson;
    
    @Column(name = "indice_atual", nullable = false)
    private Integer indiceAtual = 0;
    
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();
    
    @Column(name = "atualizado_em")
    private Instant atualizadoEm;
    
    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = Instant.now();
    }
}

