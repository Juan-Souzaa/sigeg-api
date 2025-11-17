package com.siseg.mapper;

import com.siseg.dto.pagamento.AsaasCustomerRequestDTO;
import com.siseg.dto.pagamento.AsaasPaymentRequestDTO;
import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Pagamento;
import com.siseg.model.enumerations.MetodoPagamento;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class PagamentoMapper {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String BILLING_TYPE_PIX = "PIX";
    private static final String BILLING_TYPE_CREDIT_CARD = "CREDIT_CARD";
    private static final int DIAS_VENCIMENTO = 1;
    
    public AsaasPaymentRequestDTO toAsaasPaymentRequest(Pagamento pagamento, String asaasCustomerId, CartaoCreditoRequestDTO cartaoDTO, Cliente cliente, String cpfCnpj, String remoteIp) {
        AsaasPaymentRequestDTO request = new AsaasPaymentRequestDTO();
        request.setCustomer(asaasCustomerId);
        request.setValue(pagamento.getValor().toString());
        request.setDueDate(LocalDate.now().plusDays(DIAS_VENCIMENTO).format(DATE_FORMATTER));
        request.setDescription("Pedido SIGEG #" + pagamento.getPedido().getId());
        request.setExternalReference(pagamento.getPedido().getId().toString());
        
        if (pagamento.getMetodo() == MetodoPagamento.CREDIT_CARD && cartaoDTO != null) {
            request.setBillingType(BILLING_TYPE_CREDIT_CARD);
            request.setCreditCard(toAsaasCreditCardRequest(cartaoDTO));
            if (cliente != null) {
                request.setCreditCardHolderInfo(toAsaasCreditCardHolderInfo(cliente, cpfCnpj));
            }
            if (remoteIp != null && !remoteIp.isEmpty()) {
                request.setRemoteIp(remoteIp);
            }
        } else {
            request.setBillingType(BILLING_TYPE_PIX);
        }
        
        return request;
    }
    
    public AsaasPaymentRequestDTO toAsaasPaymentRequest(Pagamento pagamento, String asaasCustomerId, CartaoCreditoRequestDTO cartaoDTO, Cliente cliente, String cpfCnpj) {
        return toAsaasPaymentRequest(pagamento, asaasCustomerId, cartaoDTO, cliente, cpfCnpj, null);
    }
    
    public AsaasPaymentRequestDTO toAsaasPaymentRequest(Pagamento pagamento, String asaasCustomerId, CartaoCreditoRequestDTO cartaoDTO) {
        return toAsaasPaymentRequest(pagamento, asaasCustomerId, cartaoDTO, null, null);
    }
    
    public AsaasPaymentRequestDTO toAsaasPaymentRequest(Pagamento pagamento, String asaasCustomerId) {
        return toAsaasPaymentRequest(pagamento, asaasCustomerId, null, null, null);
    }
    
    private AsaasPaymentRequestDTO.CreditCardDTO toAsaasCreditCardRequest(CartaoCreditoRequestDTO cartaoDTO) {
        AsaasPaymentRequestDTO.CreditCardDTO creditCard = new AsaasPaymentRequestDTO.CreditCardDTO();
        creditCard.setHolderName(cartaoDTO.getNomeTitular());
        creditCard.setNumber(cartaoDTO.getNumero());
        
        String[] validadeParts = cartaoDTO.getValidade().split("/");
        creditCard.setExpiryMonth(validadeParts[0]);
        creditCard.setExpiryYear("20" + validadeParts[1]);
        creditCard.setCcv(cartaoDTO.getCvv());
        
        return creditCard;
    }
    
    private AsaasPaymentRequestDTO.CreditCardHolderInfoDTO toAsaasCreditCardHolderInfo(Cliente cliente, String cpfCnpj) {
        AsaasPaymentRequestDTO.CreditCardHolderInfoDTO holderInfo = new AsaasPaymentRequestDTO.CreditCardHolderInfoDTO();
        holderInfo.setName(cliente.getNome());
        holderInfo.setEmail(cliente.getEmail());
        holderInfo.setCpfCnpj(cpfCnpj);
        holderInfo.setPhone(extrairTelefoneNumerico(cliente.getTelefone()));
        holderInfo.setMobilePhone(extrairTelefoneNumerico(cliente.getTelefone()));
        
        // Usar endereço principal do cliente diretamente
        cliente.getEnderecoPrincipal()
                .ifPresent(endereco -> {
                    holderInfo.setPostalCode(endereco.getCep());
                    holderInfo.setAddressNumber(endereco.getNumero());
                    holderInfo.setAddressComplement(endereco.getComplemento());
                });
        
        return holderInfo;
    }
    
    private String extrairTelefoneNumerico(String telefone) {
        if (telefone == null) {
            return null;
        }
        return telefone.replaceAll("[^0-9]", "");
    }
    
    public AsaasCustomerRequestDTO toAsaasCustomerRequest(Cliente cliente, String cpfCnpj) {
        AsaasCustomerRequestDTO request = new AsaasCustomerRequestDTO();
        request.setName(cliente.getNome());
        request.setEmail(cliente.getEmail());
        request.setPhone(extrairTelefoneNumerico(cliente.getTelefone()));
        request.setCpfCnpj(cpfCnpj);
        return request;
    }
}

