package com.siseg.model;

import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pagamentos")
@Getter
@Setter
@NoArgsConstructor
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPagamento status = StatusPagamento.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(precision = 10, scale = 2)
    private BigDecimal troco;

    // Campos específicos do PIX
    private String qrCode;
    
    @Column(columnDefinition = "LONGTEXT")
    private String qrCodeImageUrl;
    private String asaasPaymentId;
    private String asaasCustomerId;

    // Campos de reembolso
    @Column(precision = 10, scale = 2)
    private BigDecimal valorReembolsado;
    
    private Instant dataReembolso;
    
    private String asaasRefundId;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    private Instant atualizadoEm;
}
