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
    public PagamentoResponseDTO criarPagamento(Long pedidoId, CartaoCreditoRequestDTO cartaoDTO) {
        Pedido pedido = buscarPedidoValido(pedidoId);
        validatePedidoOwnership(pedido);
        pagamentoValidator.validateStatusPedido(pedido);
        
        Pagamento pagamento = criarPagamentoBasico(pedido);
        
        if (pedido.getMetodoPagamento() == MetodoPagamento.PIX) {
            processarPagamentoPix(pagamento);
        } else if (pedido.getMetodoPagamento() == MetodoPagamento.CREDIT_CARD) {
            if (cartaoDTO == null) {
                throw new IllegalArgumentException("Dados do cartão são obrigatórios para pagamento com cartão de crédito");
            }
            processarPagamentoCartao(pagamento, cartaoDTO);
        } else {
            processarPagamentoDinheiro(pagamento, pedido);
        }
        
        Pagamento saved = pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);
        
        PagamentoResponseDTO response = modelMapper.map(saved, PagamentoResponseDTO.class);
        response.setPedidoId(saved.getPedido().getId());
        return response;
    }
    
    @Transactional
    public PagamentoResponseDTO criarPagamento(Long pedidoId) {
        return criarPagamento(pedidoId, null);
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
    
    private void processarPagamentoCartao(Pagamento pagamento, CartaoCreditoRequestDTO cartaoDTO) {
        try {
            String asaasCustomerId = buscarOuCriarClienteAsaas(pagamento.getPedido().getCliente());
            AsaasPaymentResponseDTO response = criarPagamentoAsaasComCartao(pagamento, asaasCustomerId, cartaoDTO);
            
            validarRespostaAsaas(response);
            atualizarPagamentoComRespostaAsaas(pagamento, asaasCustomerId, response);
            
            if (response.getStatus() != null && "CONFIRMED".equals(response.getStatus())) {
                pagamento.setStatus(StatusPagamento.AUTHORIZED);
                pagamento.getPedido().setStatus(StatusPedido.CONFIRMED);
            } else {
                pagamento.setStatus(StatusPagamento.PENDING);
            }
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão com API Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao criar pagamento com cartão: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar pagamento com cartão de crédito: " + e.getMessage());
        }
    }
    
    private AsaasPaymentResponseDTO criarPagamentoAsaasComCartao(Pagamento pagamento, String asaasCustomerId, CartaoCreditoRequestDTO cartaoDTO) {
        AsaasPaymentRequestDTO request = criarRequestPagamentoAsaas(pagamento, asaasCustomerId, cartaoDTO);
        
        return webClient.post()
                .uri("payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AsaasPaymentResponseDTO.class)
                .block();
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
    
    private AsaasPaymentRequestDTO criarRequestPagamentoAsaas(Pagamento pagamento, String asaasCustomerId, CartaoCreditoRequestDTO cartaoDTO) {
        Cliente cliente = pagamento.getPedido().getCliente();
        String cpfCnpj = obterCpfCnpjCliente(cliente);
        return pagamentoMapper.toAsaasPaymentRequest(pagamento, asaasCustomerId, cartaoDTO, cliente, cpfCnpj);
    }
    
    private AsaasPaymentRequestDTO criarRequestPagamentoAsaas(Pagamento pagamento, String asaasCustomerId) {
        return criarRequestPagamentoAsaas(pagamento, asaasCustomerId, null);
    }
    
    private String obterCpfCnpjCliente(Cliente cliente) {
        return "24971563792";
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
        if (!isEventoPagamentoValido(webhook)) {
            return;
        }
        
        String asaasPaymentId = webhook.getPayment().getId();
        Pagamento pagamento = buscarPagamentoPorAsaasId(asaasPaymentId);
        
        String evento = webhook.getEvent();
        if ("PAYMENT_RECEIVED".equals(evento) || "PAYMENT_CONFIRMED".equals(evento)) {
            atualizarPagamentoConfirmado(pagamento);
            atualizarPedidoConfirmado(pagamento.getPedido());
            logger.info("Pagamento confirmado via webhook: " + asaasPaymentId + " - Pedido: " + pagamento.getPedido().getId());
        } else if ("PAYMENT_REFUSED".equals(evento)) {
            pagamento.setStatus(StatusPagamento.REFUSED);
            logger.warning("Pagamento recusado via webhook: " + asaasPaymentId + " - Pedido: " + pagamento.getPedido().getId());
        }
        
        pagamentoRepository.save(pagamento);
        if (pagamento.getPedido() != null) {
            pedidoRepository.save(pagamento.getPedido());
        }
    }
    
    private boolean isEventoPagamentoValido(AsaasWebhookDTO webhook) {
        if (webhook == null || webhook.getEvent() == null || webhook.getPayment() == null) {
            return false;
        }
        String evento = webhook.getEvent();
        return "PAYMENT_RECEIVED".equals(evento) || 
               "PAYMENT_CONFIRMED".equals(evento) || 
               "PAYMENT_REFUSED".equals(evento);
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
        PagamentoResponseDTO response = modelMapper.map(pagamento, PagamentoResponseDTO.class);
        response.setPedidoId(pagamento.getPedido().getId());
        return response;
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
