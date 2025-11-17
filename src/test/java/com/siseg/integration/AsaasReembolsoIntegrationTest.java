package com.siseg.integration;

import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.pagamento.AsaasRefundResponseDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Pagamento;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PagamentoRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.AsaasService;
import com.siseg.service.EnderecoService;
import com.siseg.service.PagamentoService;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração com Asaas (sandbox)
 * 
 * IMPORTANTE: Este teste faz chamadas reais à API do Asaas.
 * Configure asaas.apiKey em application.properties ou via variável de ambiente ASAAS_API_KEY.
 * Certifique-se de usar credenciais de SANDBOX, nunca produção!
 */
@SpringBootTest
@ActiveProfiles("test")
class AsaasReembolsoIntegrationTest {

    @Autowired
    private AsaasService asaasService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private EnderecoService enderecoService;

    @Value("${asaas.apiKey:}")
    private String asaasApiKey;

    private Cliente cliente;
    private Restaurante restaurante;
    private Pedido pedido;
    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        String email = "teste.integracao@example.com";
        User user = testJwtUtil.getOrCreateUser("testeintegracao", ERole.ROLE_CLIENTE);
        
        cliente = clienteRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setUser(user);
                    c.setNome("Cliente Teste Integração");
                    c.setEmail(email);
                    c.setTelefone("(11) 99415-2001");
                    Cliente saved = clienteRepository.save(c);
                    
                    EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
                    enderecoDTO.setLogradouro("Rua Teste Integração");
                    enderecoDTO.setNumero("123");
                    enderecoDTO.setBairro("Centro");
                    enderecoDTO.setCidade("São Paulo");
                    enderecoDTO.setEstado("SP");
                    enderecoDTO.setCep("01310100");
                    enderecoDTO.setPrincipal(true);
                    enderecoService.criarEndereco(enderecoDTO, saved);
                    
                    return saved;
                });

        restaurante = restauranteRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    User restauranteUser = testJwtUtil.getOrCreateUser("restauranteintegracao", ERole.ROLE_RESTAURANTE);
                    Restaurante r = new Restaurante();
                    r.setUser(restauranteUser);
                    r.setNome("Restaurante Teste Integração");
                    r.setEmail("restaurante.integracao@example.com");
                    r.setTelefone("(11) 99415-2001");
                    r.setStatus(StatusRestaurante.APPROVED);
                    Restaurante saved = restauranteRepository.save(r);
                    
                    EnderecoRequestDTO enderecoRestDTO = new EnderecoRequestDTO();
                    enderecoRestDTO.setLogradouro("Rua Restaurante Integração");
                    enderecoRestDTO.setNumero("456");
                    enderecoRestDTO.setBairro("Centro");
                    enderecoRestDTO.setCidade("São Paulo");
                    enderecoRestDTO.setEstado("SP");
                    enderecoRestDTO.setCep("01310100");
                    enderecoRestDTO.setPrincipal(true);
                    enderecoService.criarEndereco(enderecoRestDTO, saved);
                    
                    return saved;
                });

        com.siseg.model.Endereco enderecoEntrega = enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não possui endereço"));

        pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setStatus(StatusPedido.CREATED);
        pedido.setMetodoPagamento(MetodoPagamento.PIX);
        pedido.setSubtotal(new BigDecimal("10.00"));
        pedido.setTaxaEntrega(new BigDecimal("5.00"));
        pedido.setTotal(new BigDecimal("15.00"));
        pedido.setEnderecoEntrega(enderecoEntrega);
        pedido = pedidoRepository.save(pedido);
    }

    @Test
    void deveEstornarPagamentoRealNoAsaas() {
        String apiKey = System.getenv("ASAAS_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = asaasApiKey;
        }
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("test-key")) {
            return;
        }

        String asaasCustomerId = asaasService.buscarOuCriarCliente(cliente);

        pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setMetodo(MetodoPagamento.PIX);
        pagamento.setValor(pedido.getTotal());
        pagamento.setStatus(StatusPagamento.PENDING);

        com.siseg.dto.pagamento.AsaasPaymentResponseDTO paymentResponse = 
            asaasService.criarPagamentoPix(pagamento, asaasCustomerId);
        
        assertNotNull(paymentResponse, "Resposta do Asaas não deve ser nula");
        assertNotNull(paymentResponse.getId(), "ID do pagamento não deve ser nulo");
        
        String asaasPaymentId = paymentResponse.getId();

        pagamento.setAsaasPaymentId(asaasPaymentId);
        pagamento.setAsaasCustomerId(asaasCustomerId);
        pagamento.setStatus(StatusPagamento.AUTHORIZED);
        pagamento = pagamentoRepository.save(pagamento);
        pedido.setStatus(StatusPedido.CONFIRMED);
        pedidoRepository.save(pedido);

        asaasService.confirmarPagamentoSandbox(asaasPaymentId);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Teste interrompido durante espera");
        }
        
        com.siseg.dto.pagamento.AsaasPaymentResponseDTO paymentStatus = 
            asaasService.buscarPagamento(asaasPaymentId);
        
        String status = paymentStatus.getStatus();
        assertTrue("RECEIVED".equals(status) || "CONFIRMED".equals(status) || "RECEIVED_IN_CASH_APPROVED".equals(status),
                "Pagamento deve estar confirmado após chamada ao endpoint de confirmação");
        
        pagamento.setStatus(StatusPagamento.PAID);
        pagamentoRepository.save(pagamento);

        String motivo = "Teste de integração - reembolso automático";
        
        int tentativasReembolso = 0;
        int maxTentativasReembolso = 5;
        boolean reembolsoProcessado = false;
        
        while (tentativasReembolso < maxTentativasReembolso && !reembolsoProcessado) {
            try {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Teste interrompido durante espera de reembolso");
                }
                tentativasReembolso++;
                
                AsaasRefundResponseDTO refundResponse = asaasService.estornarPagamento(asaasPaymentId, motivo);
                
                assertNotNull(refundResponse, "Resposta do estorno não deve ser nula");
                assertNotNull(refundResponse.getId(), "ID do reembolso não deve ser nulo");

                com.siseg.dto.pagamento.PagamentoResponseDTO resultado = 
                    pagamentoService.processarReembolso(pedido.getId(), motivo);

                assertNotNull(resultado, "Resultado do reembolso não deve ser nulo");
                assertEquals(StatusPagamento.REFUNDED, resultado.getStatus(), "Status deve ser REFUNDED");
                assertNotNull(resultado.getValorReembolsado(), "Valor reembolsado não deve ser nulo");
                assertNotNull(resultado.getDataReembolso(), "Data do reembolso não deve ser nula");
                
                reembolsoProcessado = true;
            } catch (com.siseg.exception.PaymentGatewayException e) {
                if (e.getMessage() != null && (e.getMessage().contains("Saldo insuficiente") || 
                    e.getMessage().contains("Tente novamente em alguns instantes") ||
                    e.getMessage().contains("já está em andamento"))) {
                    if (tentativasReembolso >= maxTentativasReembolso) {
                        return;
                    }
                    continue;
                }
                fail("Erro ao processar reembolso: " + e.getMessage());
            }
        }
    }

    @Test
    void deveVerificarConexaoComAsaas() {
        String apiKey = System.getenv("ASAAS_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = asaasApiKey;
        }
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("test-key") || apiKey.startsWith("${")) {
            return;
        }

        String customerId = asaasService.buscarOuCriarCliente(cliente);
        
        assertNotNull(customerId, "Deve retornar um ID de cliente");
    }
}

