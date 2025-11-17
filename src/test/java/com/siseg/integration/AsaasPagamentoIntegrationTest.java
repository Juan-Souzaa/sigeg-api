package com.siseg.integration;

import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.pagamento.AsaasPaymentResponseDTO;
import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Pagamento;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.UserAuthenticated;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AsaasPagamentoIntegrationTest {

    @Autowired
    private AsaasService asaasService;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private EnderecoService enderecoService;

    @Value("${asaas.apiKey:}")
    private String asaasApiKey;

    private Cliente cliente;
    private Restaurante restaurante;
    private Pedido pedidoPix;
    private Pedido pedidoCartao;

    @BeforeEach
    @Transactional
    void setUp() {
        String email = "teste.pagamento@example.com";
        User user = testJwtUtil.getOrCreateUser("testepagamento", ERole.ROLE_CLIENTE);
        
        cliente = clienteRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setUser(user);
                    c.setNome("Cliente Teste Pagamento");
                    c.setEmail(email);
                    c.setTelefone("(11) 99415-2001");
                    Cliente saved = clienteRepository.save(c);
                    
                    EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
                    enderecoDTO.setLogradouro("Rua Teste Pagamento");
                    enderecoDTO.setNumero("789");
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
                    User restauranteUser = testJwtUtil.getOrCreateUser("restaurantepagamento", ERole.ROLE_RESTAURANTE);
                    Restaurante r = new Restaurante();
                    r.setUser(restauranteUser);
                    r.setNome("Restaurante Teste Pagamento");
                    r.setEmail("restaurante.pagamento@example.com");
                    r.setTelefone("(11) 99415-2001");
                    r.setStatus(StatusRestaurante.APPROVED);
                    Restaurante saved = restauranteRepository.save(r);
                    
                    EnderecoRequestDTO enderecoRestDTO = new EnderecoRequestDTO();
                    enderecoRestDTO.setLogradouro("Rua Restaurante Pagamento");
                    enderecoRestDTO.setNumero("999");
                    enderecoRestDTO.setBairro("Centro");
                    enderecoRestDTO.setCidade("São Paulo");
                    enderecoRestDTO.setEstado("SP");
                    enderecoRestDTO.setCep("01310100");
                    enderecoRestDTO.setPrincipal(true);
                    enderecoService.criarEndereco(enderecoRestDTO, saved);
                    
                    return saved;
                });

        Endereco enderecoEntrega = enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não possui endereço"));

        pedidoPix = new Pedido();
        pedidoPix.setCliente(cliente);
        pedidoPix.setRestaurante(restaurante);
        pedidoPix.setStatus(StatusPedido.CREATED);
        pedidoPix.setMetodoPagamento(MetodoPagamento.PIX);
        pedidoPix.setSubtotal(new BigDecimal("20.00"));
        pedidoPix.setTaxaEntrega(new BigDecimal("5.00"));
        pedidoPix.setTotal(new BigDecimal("25.00"));
        pedidoPix.setEnderecoEntrega(enderecoEntrega);
        pedidoPix = pedidoRepository.save(pedidoPix);

        pedidoCartao = new Pedido();
        pedidoCartao.setCliente(cliente);
        pedidoCartao.setRestaurante(restaurante);
        pedidoCartao.setStatus(StatusPedido.CREATED);
        pedidoCartao.setMetodoPagamento(MetodoPagamento.CREDIT_CARD);
        pedidoCartao.setSubtotal(new BigDecimal("30.00"));
        pedidoCartao.setTaxaEntrega(new BigDecimal("5.00"));
        pedidoCartao.setTotal(new BigDecimal("35.00"));
        pedidoCartao.setEnderecoEntrega(enderecoEntrega);
        pedidoCartao = pedidoRepository.save(pedidoCartao);
    }

    @Test
    void deveCriarPagamentoPixRealNoAsaas() {
        String apiKey = System.getenv("ASAAS_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = asaasApiKey;
        }
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("test-key")) {
            return;
        }

        User user = cliente.getUser();
        user.getRoles().size();
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userAuthenticated, null, userAuthenticated.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        PagamentoResponseDTO response = pagamentoService.criarPagamento(pedidoPix.getId(), null, "127.0.0.1");
        
        assertNotNull(response, "Resposta do pagamento não deve ser nula");
        assertNotNull(response.getId(), "ID do pagamento não deve ser nulo");
        assertEquals(MetodoPagamento.PIX, response.getMetodo(), "Método deve ser PIX");
        assertEquals(new BigDecimal("25.00"), response.getValor(), "Valor deve ser 25.00");
        assertNotNull(response.getQrCode(), "QR Code não deve ser nulo");
        assertNotNull(response.getQrCodeImageUrl(), "URL da imagem do QR Code não deve ser nula");
        
        Pagamento pagamento = pagamentoRepository.findById(response.getId())
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));
        
        assertNotNull(pagamento.getAsaasPaymentId(), "Asaas Payment ID não deve ser nulo");
        
        AsaasPaymentResponseDTO asaasPayment = asaasService.buscarPagamento(pagamento.getAsaasPaymentId());
        assertNotNull(asaasPayment, "Pagamento deve existir no Asaas");
        assertEquals("PIX", asaasPayment.getBillingType(), "Tipo de pagamento deve ser PIX");
        assertTrue("25.00".equals(asaasPayment.getValue()) || "25".equals(asaasPayment.getValue()), 
            "Valor no Asaas deve ser 25.00 ou 25");
    }

    @Test
    @Transactional
    void deveCriarPagamentoCartaoRealNoAsaas() {
        String apiKey = System.getenv("ASAAS_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = asaasApiKey;
        }
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("test-key")) {
            return;
        }

        User user = cliente.getUser();
        user.getRoles().size();
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userAuthenticated, null, userAuthenticated.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CartaoCreditoRequestDTO cartaoDTO = new CartaoCreditoRequestDTO();
        cartaoDTO.setNumero("4111111111111111");
        cartaoDTO.setNomeTitular("CLIENTE TESTE");
        cartaoDTO.setValidade("12/25");
        cartaoDTO.setCvv("123");

        PagamentoResponseDTO response = pagamentoService.criarPagamento(
            pedidoCartao.getId(), 
            cartaoDTO, 
            "127.0.0.1"
        );
        
        assertNotNull(response, "Resposta do pagamento não deve ser nula");
        assertNotNull(response.getId(), "ID do pagamento não deve ser nulo");
        assertEquals(MetodoPagamento.CREDIT_CARD, response.getMetodo(), "Método deve ser CREDIT_CARD");
        assertEquals(new BigDecimal("35.00"), response.getValor(), "Valor deve ser 35.00");
        
        Pagamento pagamento = pagamentoRepository.findById(response.getId())
                .orElseThrow(() -> new RuntimeException("Pagamento não encontrado"));
        
        if (pagamento.getAsaasPaymentId() != null && pagamento.getAsaasPaymentId().startsWith("pay_")) {
            AsaasPaymentResponseDTO asaasPayment = asaasService.buscarPagamento(pagamento.getAsaasPaymentId());
            assertNotNull(asaasPayment, "Pagamento deve existir no Asaas");
            assertEquals("CREDIT_CARD", asaasPayment.getBillingType(), "Tipo de pagamento deve ser CREDIT_CARD");
            assertTrue("35.00".equals(asaasPayment.getValue()) || "35".equals(asaasPayment.getValue()), 
                "Valor no Asaas deve ser 35.00 ou 35");
        }
    }

    @Test
    void deveCriarPagamentoPixViaAsaasService() {
        String apiKey = System.getenv("ASAAS_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = asaasApiKey;
        }
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("test-key")) {
            return;
        }

        String asaasCustomerId = asaasService.buscarOuCriarCliente(cliente);
        assertNotNull(asaasCustomerId, "Customer ID não deve ser nulo");

        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedidoPix);
        pagamento.setMetodo(MetodoPagamento.PIX);
        pagamento.setValor(pedidoPix.getTotal());
        pagamento.setStatus(StatusPagamento.PENDING);

        AsaasPaymentResponseDTO response = asaasService.criarPagamentoPix(pagamento, asaasCustomerId);
        
        assertNotNull(response, "Resposta do Asaas não deve ser nula");
        assertNotNull(response.getId(), "ID do pagamento não deve ser nulo");
        assertEquals("PIX", response.getBillingType(), "Tipo de pagamento deve ser PIX");
        assertTrue("25.00".equals(response.getValue()) || "25".equals(response.getValue()), 
            "Valor deve ser 25.00 ou 25");
        
        AsaasPaymentResponseDTO paymentVerification = asaasService.buscarPagamento(response.getId());
        assertNotNull(paymentVerification, "Pagamento deve existir no Asaas");
        assertEquals(response.getId(), paymentVerification.getId(), "IDs devem ser iguais");
    }
}

