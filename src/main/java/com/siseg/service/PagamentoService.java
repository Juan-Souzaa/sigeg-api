package com.siseg.service;

import com.siseg.dto.pagamento.*;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.exception.PaymentGatewayException;
import com.siseg.model.Cliente;
import com.siseg.model.Pagamento;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.*;
import com.siseg.util.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

@Service
public class PagamentoService {
    
    private static final Logger logger = Logger.getLogger(PagamentoService.class.getName());
    
    @Value("${asaas.apiKey}")
    private String asaasApiKey;
    
    @Value("${asaas.baseUrl}")
    private String asaasBaseUrl;
    
    @Value("${asaas.webhookSecret}")
    private String webhookSecret;
    
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;
    
    public PagamentoService(PagamentoRepository pagamentoRepository, PedidoRepository pedidoRepository,
                           ClienteRepository clienteRepository, ModelMapper modelMapper, @Value("${asaas.baseUrl}") String asaasBaseUrl, @Value("${asaas.apiKey}") String asaasApiKey ) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.modelMapper = modelMapper;
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
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        // Valida se o pedido pertence ao usuário autenticado
        validatePedidoOwnership(pedido);
        
        if (pedido.getStatus() != StatusPedido.CREATED) {
            throw new RuntimeException("Pedido já foi processado");
        }
        
        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setMetodo(pedido.getMetodoPagamento());
        pagamento.setValor(pedido.getTotal());
        pagamento.setTroco(pedido.getTroco());
        pagamento.setStatus(StatusPagamento.PENDING);
        
        if (pedido.getMetodoPagamento() == MetodoPagamento.PIX) {
            processarPagamentoPix(pagamento);
        } else {
            // Pagamento em dinheiro - apenas confirma o pedido
            pagamento.setStatus(StatusPagamento.PENDING);
            pedido.setStatus(StatusPedido.CONFIRMED);
        }
        
        Pagamento saved = pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);
        
        return modelMapper.map(saved, PagamentoResponseDTO.class);
    }
    
    private void processarPagamentoPix(Pagamento pagamento) {
        try {
            // Buscar ou criar cliente no Asaas
            String asaasCustomerId = buscarOuCriarClienteAsaas(pagamento.getPedido().getCliente());
            
            // Criar pagamento PIX no Asaas
            AsaasPaymentRequestDTO request = new AsaasPaymentRequestDTO();
            request.setCustomer(asaasCustomerId);
            request.setBillingType("PIX");
            request.setValue(pagamento.getValor().toString());
            request.setDueDate(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            request.setDescription("Pedido SIGEG #" + pagamento.getPedido().getId());
            request.setExternalReference(pagamento.getPedido().getId().toString());
            
            AsaasPaymentResponseDTO response = webClient.post()
                    .uri("/payments")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AsaasPaymentResponseDTO.class)
                    .block();
            
            if (response != null) {
                pagamento.setAsaasPaymentId(response.getId());
                pagamento.setAsaasCustomerId(asaasCustomerId);
                pagamento.setStatus(StatusPagamento.AUTHORIZED);
                
                // Obter QR Code usando o endpoint correto
                obterQrCodePix(pagamento, response.getId());
            } else {
                throw new PaymentGatewayException("Resposta nula da API Asaas");
            }
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão com API Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao criar pagamento PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar pagamento PIX: " + e.getMessage());
        }
    }
    
    private void obterQrCodePix(Pagamento pagamento, String asaasPaymentId) {
        try {
            AsaasQrCodeResponseDTO qrCodeResponse = webClient.get()
                    .uri("/payments/{id}/pixQrCode", asaasPaymentId)
                    .retrieve()
                    .bodyToMono(AsaasQrCodeResponseDTO.class)
                    .block();
            
            if (qrCodeResponse != null) {
                pagamento.setQrCode(qrCodeResponse.getPayload()); // Copia e Cola
                pagamento.setQrCodeImageUrl(qrCodeResponse.getEncodedImage()); // Imagem Base64
            }
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão ao obter QR Code PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao obter QR Code PIX: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao obter QR Code PIX: " + e.getMessage());
        }
    }
    
    private String buscarOuCriarClienteAsaas(Cliente cliente) {
        try {
            
            // Buscar cliente existente por email
            AsaasCustomerResponseDTO existingCustomer = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/customers")
                            .queryParam("email", cliente.getEmail())
                            .build())
                    .retrieve()
                    .bodyToMono(AsaasCustomerResponseDTO.class)
                    .block();
            
            if (existingCustomer != null && existingCustomer.getId() != null) {
                return existingCustomer.getId();
            }
            
            
            // Criar novo cliente
            AsaasCustomerRequestDTO customerRequest = new AsaasCustomerRequestDTO();
            customerRequest.setName(cliente.getNome());
            customerRequest.setEmail(cliente.getEmail());
            customerRequest.setPhone(cliente.getTelefone());
            customerRequest.setCpfCnpj("24971563792"); // CPF genérico para teste
            
            AsaasCustomerResponseDTO newCustomer = webClient.post()
                    .uri("/customers")
                    .bodyValue(customerRequest)
                    .retrieve()
                    .bodyToMono(AsaasCustomerResponseDTO.class)
                    .block();
            
            if (newCustomer != null && newCustomer.getId() != null) {
                return newCustomer.getId();
            } else {
                throw new PaymentGatewayException("Falha ao criar cliente no Asaas - resposta nula");
            }
            
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            logger.severe("Erro de conexão ao buscar/criar cliente no Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro de conexão com o gateway de pagamento. Verifique sua conexão com a internet.", e);
        } catch (Exception e) {
            logger.severe("Erro ao buscar/criar cliente no Asaas: " + e.getMessage());
            throw new PaymentGatewayException("Erro ao processar cliente: " + e.getMessage());
        }
    }
    
    @Transactional
    public void processarWebhook(AsaasWebhookDTO webhook) {
        if (!"PAYMENT_RECEIVED".equals(webhook.getEvent())) {
            return;
        }
        
        String asaasPaymentId = webhook.getPayment().getId();
        Pagamento pagamento = pagamentoRepository.findByAsaasPaymentId(asaasPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado: " + asaasPaymentId));
        
        pagamento.setStatus(StatusPagamento.PAID);
        pagamento.setAtualizadoEm(java.time.Instant.now());
        
        Pedido pedido = pagamento.getPedido();
        pedido.setStatus(StatusPedido.CONFIRMED);
        
        pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);
        
        logger.info("Pagamento confirmado via webhook: " + asaasPaymentId + " - Pedido: " + pedido.getId());
    }
    
    public PagamentoResponseDTO buscarPagamentoPorPedido(Long pedidoId) {
        Pagamento pagamento = pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado para o pedido: " + pedidoId));
        
        // Valida se o pedido do pagamento pertence ao usuário autenticado
        validatePedidoOwnership(pagamento.getPedido());
        
        return modelMapper.map(pagamento, PagamentoResponseDTO.class);
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Admin pode acessar qualquer pedido
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        // Verifica se o pedido pertence ao cliente autenticado
        if (pedido.getCliente() == null || pedido.getCliente().getUser() == null || 
            !pedido.getCliente().getUser().getId().equals(currentUser.getId())) {
            
            // Também verifica se é dono do restaurante
            if (pedido.getRestaurante() == null || pedido.getRestaurante().getUser() == null ||
                !pedido.getRestaurante().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Você não tem permissão para acessar este pagamento");
            }
        }
    }
    
    public boolean validarWebhookSignature(String signature, String payload) {
       
        return signature != null && signature.equals(webhookSecret);
    }
}
