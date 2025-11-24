package com.siseg.dto.ganhos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioCompletoDTO {
    
    private BigDecimal totalVendas;
    private Long totalPedidos;
    private BigDecimal ticketMedio;
    private BigDecimal taxaEntregaMedia;
    
   
    private BigDecimal distribuicaoRestaurantes;
    private BigDecimal distribuicaoEntregadores;
    private BigDecimal taxaPlataforma;
    
  
    private Long totalClientes;
    private Long qtdRestaurantes;
    private Long qtdEntregadores;
    
   
    private BigDecimal pedidosPorCliente;
    private BigDecimal taxaConversao;
    private String periodo;
    private String tendencia;
}

