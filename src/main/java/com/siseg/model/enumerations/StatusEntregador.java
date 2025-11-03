package com.siseg.model.enumerations;

public enum StatusEntregador {
    PENDING_APPROVAL,  // Aguardando aprovação do admin
    APPROVED,          // Aprovado e disponível para trabalhar
    REJECTED,          // Rejeitado pelo admin
    UNAVAILABLE,       // Entregador offline ou não disponível
    AVAILABLE          // Entregador online e disponível para entregas
}

