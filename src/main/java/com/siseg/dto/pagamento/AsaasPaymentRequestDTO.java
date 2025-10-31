package com.siseg.dto.pagamento;

import lombok.Data;

@Data
public class AsaasPaymentRequestDTO {
    private String customer;
    private String billingType;
    private String value;
    private String dueDate;
    private String description;
    private String externalReference;
}
