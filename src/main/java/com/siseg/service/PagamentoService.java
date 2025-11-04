package com.siseg.service;

import com.siseg.dto.pagamento.*;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.exception.PaymentGatewayException;
import com.siseg.model.Cliente;
import com.siseg.model.Pagamento;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.*;
import com.siseg.mapper.PagamentoMapper;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.PagamentoValidator;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.logging.Logger;

@Service
public class PagamentoService {
    
    private static final Logger logger = Logger.getLogger(PagamentoService.class.getName());
    
    @Value("${asaas.webhookSecret}")
    private String webhookSecret;
    
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final ModelMapper modelMapper;
    private final PagamentoMapper pagamentoMapper;
    private final PagamentoValidator pagamentoValidator;
    private final WebClient webClient;
    
    public PagamentoService(PagamentoRepository pagamentoRepository, PedidoRepository pedidoRepository,
                           ModelMapper modelMapper, PagamentoMapper pagamentoMapper,
                           PagamentoValidator pagamentoValidator,
                           @Value("${asaas.baseUrl}") String asaasBaseUrl, @Value("${asaas.apiKey}") String asaasApiKey ) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.modelMapper = modelMapper;
        this.pagamentoMapper = pagamentoMapper;
        this.pagamentoValidator = pagamentoValidator;
        this.webClient = WebClient.builder()
                .baseUrl(asaasBaseUrl)
                .defaultHeader("access_token", asaasApiKey) 
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "SIGEG-App/1.0")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
    
    @Transactional
    public PagamentoResponseDTO criarPagamento(Long pedidoId) {
        Pedido pedido = buscarPedidoValido(pedidoId);
        validatePedidoOwnership(pedido);
        pagamentoValidator.validateStatusPedido(pedido);
        
        Pagamento pagamento = criarPagamentoBasico(pedido);
        
        if (pedido.getMetodoPagamento() == MetodoPagamento.PIX) {
            processarPagamentoPix(pagamento);
        } else {
            processarPagamentoDinheiro(pagamento, pedido);
        }
        
        Pagamento saved = pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);
        
        return modelMapper.map(saved, PagamentoResponseDTO.class);
    }
    
    private Pedido buscarPedidoValido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
    }
    
    private Pagamento criarPagamentoBasico(Pedido pedido) {
        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setMetodo(pedido.getMetodoPagamento());
        pagamento.setValor(pedido.getTotal());
        pagamento.setTroco(pedido.getTroco());
        pagamento.setStatus(StatusPagamento.PENDING);
        return pagamento;
    }
    
    private void processarPagamentoDinheiro(Pagamento pagamento, Pedido pedido) {
        pagamento.setStatus(StatusPagamento.PENDING);
        pedido.setStatus(StatusPedido.CONFIRMED);
    }
    
    private void processarPagamentoPix(Pagamento pagamento) {
        try {
            String asaasCustomerId = buscarOuCriarClienteAsaas(pagamento.getPedido().getCliente());
            AsaasPaymentResponseDTO response = criarPagamentoAsaas(pagamento, asaasCustomerId);
            
            validarRespostaAsaas(response);
            atualizarPagamentoComRespostaAsaas(pagamento, asaasCustomerId, response);
            obterQrCodePix(pagamento, response.getId());
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão com API Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao criar pagamento PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar pagamento PIX: " + e.getMessage());
        }
    }
    
    private void validarRespostaAsaas(AsaasPaymentResponseDTO response) {
        if (response == null) {
            throw new PaymentGatewayException("Resposta nula da API Asaas");
        }
    }
    
    private AsaasPaymentResponseDTO criarPagamentoAsaas(Pagamento pagamento, String asaasCustomerId) {
        AsaasPaymentRequestDTO request = criarRequestPagamentoAsaas(pagamento, asaasCustomerId);
        
        return webClient.post()
                .uri("/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AsaasPaymentResponseDTO.class)
                .block();
    }
    
    private AsaasPaymentRequestDTO criarRequestPagamentoAsaas(Pagamento pagamento, String asaasCustomerId) {
        return pagamentoMapper.toAsaasPaymentRequest(pagamento, asaasCustomerId);
    }
    
    private void atualizarPagamentoComRespostaAsaas(Pagamento pagamento, String asaasCustomerId, AsaasPaymentResponseDTO response) {
        pagamento.setAsaasPaymentId(response.getId());
        pagamento.setAsaasCustomerId(asaasCustomerId);
        pagamento.setStatus(StatusPagamento.AUTHORIZED);
    }
    
    private void obterQrCodePix(Pagamento pagamento, String asaasPaymentId) {
        try {
            AsaasQrCodeResponseDTO qrCodeResponse = buscarQrCodeAsaas(asaasPaymentId);
            if (qrCodeResponse != null) {
                atualizarPagamentoComQrCode(pagamento, qrCodeResponse);
            }
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão ao obter QR Code PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao obter QR Code PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao obter QR Code PIX: " + e.getMessage());
        }
    }
    
    private AsaasQrCodeResponseDTO buscarQrCodeAsaas(String asaasPaymentId) {
        return webClient.get()
                .uri("/payments/{id}/pixQrCode", asaasPaymentId)
                .retrieve()
                .bodyToMono(AsaasQrCodeResponseDTO.class)
                .block();
    }
    
    private void atualizarPagamentoComQrCode(Pagamento pagamento, AsaasQrCodeResponseDTO qrCodeResponse) {
        pagamento.setQrCode(qrCodeResponse.getPayload());
        pagamento.setQrCodeImageUrl(qrCodeResponse.getEncodedImage());
    }
    
    private String buscarOuCriarClienteAsaas(Cliente cliente) {
        try {
            String customerId = buscarClienteAsaasPorEmail(cliente.getEmail());
            if (customerId != null) {
                return customerId;
            }
            
            return criarClienteAsaas(cliente);
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão ao buscar/criar cliente no Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao buscar/criar cliente no Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar cliente: " + e.getMessage());
        }
    }
    
    private String buscarClienteAsaasPorEmail(String email) {
        AsaasCustomerResponseDTO existingCustomer = buscarClienteAsaas(email);
        
        if (temClienteValido(existingCustomer)) {
            return existingCustomer.getId();
        }
        
        return null;
    }
    
    private AsaasCustomerResponseDTO buscarClienteAsaas(String email) {
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
    
    private String criarClienteAsaas(Cliente cliente) {
        AsaasCustomerRequestDTO customerRequest = criarRequestClienteAsaas(cliente);
        AsaasCustomerResponseDTO newCustomer = criarClienteAsaasNaApi(customerRequest);
        
        if (!temClienteValido(newCustomer)) {
            throw new PaymentGatewayException("Falha ao criar cliente no Asaas - resposta nula");
        }
        
        return newCustomer.getId();
    }
    
    private AsaasCustomerResponseDTO criarClienteAsaasNaApi(AsaasCustomerRequestDTO customerRequest) {
        return webClient.post()
                .uri("/customers")
                .bodyValue(customerRequest)
                .retrieve()
                .bodyToMono(AsaasCustomerResponseDTO.class)
                .block();
    }
    
    private AsaasCustomerRequestDTO criarRequestClienteAsaas(Cliente cliente) {
        return pagamentoMapper.toAsaasCustomerRequest(cliente, "24971563792");
    }
    
    @Transactional
    public void processarWebhook(AsaasWebhookDTO webhook) {
        if (!isPagamentoRecebido(webhook)) {
            return;
        }
        
        String asaasPaymentId = webhook.getPayment().getId();
        Pagamento pagamento = buscarPagamentoPorAsaasId(asaasPaymentId);
        
        atualizarPagamentoConfirmado(pagamento);
        atualizarPedidoConfirmado(pagamento.getPedido());
        
        pagamentoRepository.save(pagamento);
        pedidoRepository.save(pagamento.getPedido());
        
        logger.info("Pagamento confirmado via webhook: " + asaasPaymentId + " - Pedido: " + pagamento.getPedido().getId());
    }
    
    private boolean isPagamentoRecebido(AsaasWebhookDTO webhook) {
        return "PAYMENT_RECEIVED".equals(webhook.getEvent());
    }
    
    private Pagamento buscarPagamentoPorAsaasId(String asaasPaymentId) {
        return pagamentoRepository.findByAsaasPaymentId(asaasPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + asaasPaymentId));
    }
    
    private void atualizarPagamentoConfirmado(Pagamento pagamento) {
        pagamento.setStatus(StatusPagamento.PAID);
        pagamento.setAtualizadoEm(java.time.Instant.now());
    }
    
    private void atualizarPedidoConfirmado(Pedido pedido) {
        pedido.setStatus(StatusPedido.CONFIRMED);
    }
    
    public PagamentoResponseDTO buscarPagamentoPorPedido(Long pedidoId) {
        Pagamento pagamento = buscarPagamentoPorPedidoId(pedidoId);
        validatePedidoOwnership(pagamento.getPedido());
        return modelMapper.map(pagamento, PagamentoResponseDTO.class);
    }
    
    private Pagamento buscarPagamentoPorPedidoId(Long pedidoId) {
        return pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado para o pedido: " + pedidoId));
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        SecurityUtils.validatePedidoOwnership(pedido);
    }
    
    public boolean validarWebhookSignature(String signature, String payload) {
        return signature != null && signature.equals(webhookSecret);
    }
}
