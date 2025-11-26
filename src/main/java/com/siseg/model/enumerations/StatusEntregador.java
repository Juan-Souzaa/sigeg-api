package com.siseg.model.enumerations;

public enum StatusEntregador {
    PENDING_APPROVAL,  // Aguardando aprovação do admin
    APPROVED,          // Aprovado e pode trabalhar
    REJECTED           // Rejeitado pelo admin
}

