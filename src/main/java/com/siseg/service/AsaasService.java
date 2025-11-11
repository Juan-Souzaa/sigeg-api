package com.siseg.service;

import com.siseg.dto.pagamento.*;
import com.siseg.exception.PaymentGatewayException;
import com.siseg.mapper.PagamentoMapper;
import com.siseg.model.Cliente;
import com.siseg.model.Pagamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.logging.Logger;

@Service
public class AsaasService {
    
    private static final Logger logger = Logger.getLogger(AsaasService.class.getName());
    
    private final WebClient webClient;
    private final PagamentoMapper pagamentoMapper;
    
    public AsaasService(@Value("${asaas.baseUrl}") String asaasBaseUrl, 
                       @Value("${asaas.apiKey}") String asaasApiKey,
                       PagamentoMapper pagamentoMapper) {
        this.pagamentoMapper = pagamentoMapper;
        this.webClient = WebClient.builder()
                .baseUrl(asaasBaseUrl)
                .defaultHeader("access_token", asaasApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "SIGEG-App/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
    
    public String buscarOuCriarCliente(Cliente cliente) {
        try {
            String customerId = buscarClientePorEmail(cliente.getEmail());
            if (customerId != null) {
                return customerId;
            }
            return criarCliente(cliente);
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão ao buscar/criar cliente no Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao buscar/criar cliente no Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar cliente: " + e.getMessage());
        }
    }
    
    public AsaasPaymentResponseDTO criarPagamentoPix(Pagamento pagamento, String asaasCustomerId) {
        AsaasPaymentRequestDTO request = criarRequestPagamento(pagamento, asaasCustomerId, null, null);
        return chamarApi("/payments", request);
    }
    
    public AsaasPaymentResponseDTO criarPagamentoCartao(Pagamento pagamento, String asaasCustomerId, 
                                                       CartaoCreditoRequestDTO cartaoDTO, String remoteIp) {
        AsaasPaymentRequestDTO request = criarRequestPagamento(pagamento, asaasCustomerId, cartaoDTO, remoteIp);
        return chamarApi("/payments", request);
    }
    
    public AsaasQrCodeResponseDTO buscarQrCodePix(String asaasPaymentId) {
        try {
            return webClient.get()
                    .uri("/payments/{id}/pixQrCode", asaasPaymentId)
                    .retrieve()
                    .bodyToMono(AsaasQrCodeResponseDTO.class)
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão ao obter QR Code PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao obter QR Code PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao obter QR Code PIX: " + e.getMessage());
        }
    }
    
    private String buscarClientePorEmail(String email) {
        AsaasCustomerResponseDTO existingCustomer = buscarCliente(email);
        if (temClienteValido(existingCustomer)) {
            return existingCustomer.getId();
        }
        return null;
    }
    
    private AsaasCustomerResponseDTO buscarCliente(String email) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/customers")
                        .queryParam("email", email)
                        .build())
                .retrieve()
                .bodyToMono(AsaasCustomerResponseDTO.class)
                .block();
    }
    
    private boolean temClienteValido(AsaasCustomerResponseDTO customer) {
        return customer != null && customer.getId() != null;
    }
    
    private String criarCliente(Cliente cliente) {
        AsaasCustomerRequestDTO customerRequest = criarRequestCliente(cliente);
        AsaasCustomerResponseDTO newCustomer = criarClienteNaApi(customerRequest);
        
        if (!temClienteValido(newCustomer)) {
            throw new PaymentGatewayException("Falha ao criar cliente no Asaas - resposta nula");
        }
        
        return newCustomer.getId();
    }
    
    private AsaasCustomerResponseDTO criarClienteNaApi(AsaasCustomerRequestDTO customerRequest) {
        return webClient.post()
                .uri("/customers")
                .bodyValue(customerRequest)
                .retrieve()
                .bodyToMono(AsaasCustomerResponseDTO.class)
                .block();
    }
    
    private AsaasCustomerRequestDTO criarRequestCliente(Cliente cliente) {
        return pagamentoMapper.toAsaasCustomerRequest(cliente, obterCpfCnpjCliente(cliente));
    }
    
    private AsaasPaymentRequestDTO criarRequestPagamento(Pagamento pagamento, String asaasCustomerId, 
                                                         CartaoCreditoRequestDTO cartaoDTO, String remoteIp) {
        Cliente cliente = pagamento.getPedido().getCliente();
        String cpfCnpj = obterCpfCnpjCliente(cliente);
        return pagamentoMapper.toAsaasPaymentRequest(pagamento, asaasCustomerId, cartaoDTO, cliente, cpfCnpj, remoteIp);
    }
    
    private AsaasPaymentResponseDTO chamarApi(String endpoint, AsaasPaymentRequestDTO request) {
        try {
            return webClient.post()
                    .uri(endpoint)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AsaasPaymentResponseDTO.class)
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            logger.severe("Erro do Asaas (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            throw e;
        }
    }
    
    private String obterCpfCnpjCliente(Cliente cliente) {
        return "24971563792";
    }
}

