package com.siseg.dto.pagamento;

import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PagamentoResponseDTO {
    private Long id;
    private Long pedidoId;
    private MetodoPagamento metodo;
    private StatusPagamento status;
    private BigDecimal valor;
    private BigDecimal troco;
    private String qrCode;
    private String qrCodeImageUrl;
    private BigDecimal valorReembolsado;
    private Instant dataReembolso;
    private String asaasRefundId;
    private Instant criadoEm;
    private Instant atualizadoEm;
}
