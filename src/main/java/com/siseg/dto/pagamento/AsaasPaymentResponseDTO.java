package com.siseg.dto.pagamento;

import lombok.Data;

@Data
public class AsaasPaymentResponseDTO {
    private String id;
    private String customer;
    private String billingType;
    private String value;
    private String status;
    private String pixTransaction;
    private String pixQrCode;
    private String pixQrCodeImage;
    private String dueDate;
    private String description;
    private String externalReference;
}
