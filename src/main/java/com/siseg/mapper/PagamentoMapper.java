package com.siseg.mapper;

import com.siseg.dto.pagamento.AsaasCustomerRequestDTO;
import com.siseg.dto.pagamento.AsaasPaymentRequestDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Pagamento;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class PagamentoMapper {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String BILLING_TYPE_PIX = "PIX";
    private static final int DIAS_VENCIMENTO = 1;
    
    public AsaasPaymentRequestDTO toAsaasPaymentRequest(Pagamento pagamento, String asaasCustomerId) {
        AsaasPaymentRequestDTO request = new AsaasPaymentRequestDTO();
        request.setCustomer(asaasCustomerId);
        request.setBillingType(BILLING_TYPE_PIX);
        request.setValue(pagamento.getValor().toString());
        request.setDueDate(LocalDate.now().plusDays(DIAS_VENCIMENTO).format(DATE_FORMATTER));
        request.setDescription("Pedido SIGEG #" + pagamento.getPedido().getId());
        request.setExternalReference(pagamento.getPedido().getId().toString());
        return request;
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

