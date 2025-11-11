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
        
        EnderecoParseado endereco = parsearEndereco(cliente.getEndereco());
        holderInfo.setPostalCode(obterCepValido(endereco.cep));
        holderInfo.setAddressNumber(obterNumeroValido(endereco.numero));
        holderInfo.setAddressComplement(endereco.complemento);
        
        return holderInfo;
    }
    
    private String obterCepValido(String cep) {
        if (cep != null && !cep.isEmpty()) {
            String cepLimpo = cep.replaceAll("[^0-9]", "");
            if (cepLimpo.length() == 8) {
                return cepLimpo;
            }
        }
        return "01310100";
    }
    
    private String obterNumeroValido(String numero) {
        if (numero != null && !numero.isEmpty()) {
            return numero;
        }
        return "0";
    }
    
    private String extrairTelefoneNumerico(String telefone) {
        if (telefone == null) {
            return null;
        }
        return telefone.replaceAll("[^0-9]", "");
    }
    
    private EnderecoParseado parsearEndereco(String endereco) {
        EnderecoParseado resultado = new EnderecoParseado();
        
        if (endereco == null || endereco.trim().isEmpty()) {
            return resultado;
        }
        
        String enderecoLimpo = endereco.trim();
        
        java.util.regex.Pattern cepPattern = java.util.regex.Pattern.compile("(\\d{5}-?\\d{3})");
        java.util.regex.Matcher cepMatcher = cepPattern.matcher(enderecoLimpo);
        if (cepMatcher.find()) {
            String cep = cepMatcher.group(1).replaceAll("[^0-9]", "");
            if (cep.length() == 8) {
                resultado.cep = cep;
            }
        } else {
            java.util.regex.Pattern cepPatternSimples = java.util.regex.Pattern.compile("(\\d{8})");
            java.util.regex.Matcher cepMatcherSimples = cepPatternSimples.matcher(enderecoLimpo);
            if (cepMatcherSimples.find()) {
                resultado.cep = cepMatcherSimples.group(1);
            }
        }
        
        java.util.regex.Pattern numeroPattern = java.util.regex.Pattern.compile("(?:,|\\s|n[oº°]?\\s*)(\\d+)(?:\\s*-\\s*(\\d+))?");
        java.util.regex.Matcher numeroMatcher = numeroPattern.matcher(enderecoLimpo);
        if (numeroMatcher.find()) {
            resultado.numero = numeroMatcher.group(1);
            if (numeroMatcher.group(2) != null) {
                resultado.complemento = numeroMatcher.group(2);
            }
        }
        
        return resultado;
    }
    
    private static class EnderecoParseado {
        String cep;
        String numero;
        String complemento;
    }
    
    public AsaasCustomerRequestDTO toAsaasCustomerRequest(Cliente cliente, String cpfCnpj) {
        AsaasCustomerRequestDTO request = new AsaasCustomerRequestDTO();
        request.setName(cliente.getNome());
        request.setEmail(cliente.getEmail());
        request.setPhone(cliente.getTelefone());
        request.setCpfCnpj(cpfCnpj);
        return request;
    }
}

