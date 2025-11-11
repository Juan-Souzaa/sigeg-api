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
    private CreditCardDTO creditCard;
    private CreditCardHolderInfoDTO creditCardHolderInfo;
    private String remoteIp;
    
    @Data
    public static class CreditCardDTO {
        private String holderName;
        private String number;
        private String expiryMonth;
        private String expiryYear;
        private String ccv;
    }
    
    @Data
    public static class CreditCardHolderInfoDTO {
        private String name;
        private String email;
        private String cpfCnpj;
        private String postalCode;
        private String addressNumber;
        private String addressComplement;
        private String phone;
        private String mobilePhone;
    }
}
