package com.siseg.service;

import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.CriarPagamentoCompletoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.dto.pagamento.ReembolsoRequestDTO;
import com.siseg.integration.pagamento.PagamentoServiceAdapter;
import com.siseg.model.Pedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

@Service
public class PagamentoServiceClient {
    
    private static final Logger logger = Logger.getLogger(PagamentoServiceClient.class.getName());
    
    private final WebClient webClient;
    private final PagamentoServiceAdapter adapter;
    
    public PagamentoServiceClient(@Value("${payment.service.url}") String paymentServiceUrl,
                                  PagamentoServiceAdapter adapter) {
        this.adapter = adapter;
        this.webClient = WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(addJwtTokenFilter())
                .build();
    }
    
    private ExchangeFilterFunction addJwtTokenFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            String jwtToken = extractJwtToken();
            
            if (jwtToken != null && !jwtToken.isEmpty()) {
                ClientRequest filteredRequest = ClientRequest.from(clientRequest)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .build();
                
                logger.fine("Token JWT adicionado ao header Authorization");
                return Mono.just(filteredRequest);
            }
            
            logger.warning("Token JWT não encontrado no header Authorization da requisição original");
            return Mono.just(clientRequest);
        });
    }
    
    private String extractJwtToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        return null;
    }
    
    public PagamentoResponseDTO criarPagamento(Pedido pedido, CartaoCreditoRequestDTO cartaoDTO, String remoteIp) {
        try {
            CriarPagamentoCompletoRequestDTO request = adapter.adaptarPedidoParaCriacaoPagamento(pedido, cartaoDTO);
            
            return webClient.post()
                    .uri("/api/pagamentos")
                    .bodyValue(request)
                    .header("X-Forwarded-For", remoteIp != null ? remoteIp : "")
                    .retrieve()
                    .bodyToMono(PagamentoResponseDTO.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            logger.severe("Erro do serviço de pagamento (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao criar pagamento: " + e.getResponseBodyAsString(), e);
        } catch (WebClientException e) {
            logger.severe("Erro de conexão com serviço de pagamento: " + e.getMessage());
            throw new RuntimeException("Erro de conexão com o serviço de pagamento. Verifique se o serviço está disponível.", e);
        } catch (Exception e) {
            logger.severe("Erro ao criar pagamento: " + e.getMessage());
            throw new RuntimeException("Erro ao criar pagamento: " + e.getMessage(), e);
        }
    }
    
    public PagamentoResponseDTO buscarPagamentoPorPedido(Long pedidoId) {
        try {
            return webClient.get()
                    .uri("/api/pagamentos/pedidos/{pedidoId}", pedidoId)
                    .retrieve()
                    .bodyToMono(PagamentoResponseDTO.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                logger.warning("Pagamento não encontrado para pedido: " + pedidoId);
                throw new com.siseg.exception.ResourceNotFoundException("Pagamento não encontrado para o pedido: " + pedidoId);
            }
            logger.severe("Erro do serviço de pagamento (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao buscar pagamento: " + e.getResponseBodyAsString(), e);
        } catch (WebClientException e) {
            logger.severe("Erro de conexão com serviço de pagamento: " + e.getMessage());
            throw new RuntimeException("Erro de conexão com o serviço de pagamento. Verifique se o serviço está disponível.", e);
        } catch (Exception e) {
            logger.severe("Erro ao buscar pagamento: " + e.getMessage());
            throw new RuntimeException("Erro ao buscar pagamento: " + e.getMessage(), e);
        }
    }
    
    public PagamentoResponseDTO processarReembolso(Long pedidoId, String motivo) {
        try {
            ReembolsoRequestDTO request = new ReembolsoRequestDTO();
            request.setMotivo(motivo);
            
            return webClient.post()
                    .uri("/api/pagamentos/pedidos/{pedidoId}/reembolso", pedidoId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PagamentoResponseDTO.class)
                    .block();
                    
        } catch (WebClientResponseException e) {
            logger.severe("Erro do serviço de pagamento (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            throw new RuntimeException("Erro ao processar reembolso: " + e.getResponseBodyAsString(), e);
        } catch (WebClientException e) {
            logger.severe("Erro de conexão com serviço de pagamento: " + e.getMessage());
            throw new RuntimeException("Erro de conexão com o serviço de pagamento. Verifique se o serviço está disponível.", e);
        } catch (Exception e) {
            logger.severe("Erro ao processar reembolso: " + e.getMessage());
            throw new RuntimeException("Erro ao processar reembolso: " + e.getMessage(), e);
        }
    }
    
}
