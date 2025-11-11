package com.siseg.model;

import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurante_id", nullable = false)
    private Restaurante restaurante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status = StatusPedido.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPagamento metodoPagamento;

    @Column(precision = 10, scale = 2)
    private BigDecimal troco;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(nullable = false)
    private String enderecoEntrega;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitudeEntrega;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitudeEntrega;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxaEntrega;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregador_id")
    private Entregador entregador;

    private Instant tempoEstimadoEntrega;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PedidoItem> itens = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column(precision = 10, scale = 2)
    private BigDecimal taxaPlataformaRestaurante;

    @Column(precision = 10, scale = 2)
    private BigDecimal taxaPlataformaEntregador;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorLiquidoRestaurante;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorLiquidoEntregador;
}
