package com.siseg.controller;

import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.EnderecoService;
import com.siseg.service.PagamentoService;
import com.siseg.service.PagamentoServiceClient;
import com.siseg.util.SecurityUtils;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagamentoControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private EnderecoService enderecoService;

    @SpyBean
    private PagamentoService pagamentoService;

    @MockBean
    private PagamentoServiceClient pagamentoServiceClient;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    private String clienteToken;
    private String restauranteToken;
    private Cliente cliente;
    private Restaurante restaurante;
    private Pedido pedido;

    @BeforeEach
    void setUp() throws Exception {
        clienteToken = testJwtUtil.generateTokenForUser("cliente", ERole.ROLE_CLIENTE);

        User user = testJwtUtil.getOrCreateUser("cliente", ERole.ROLE_CLIENTE);
        cliente = clienteRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cliente existente = clienteRepository.findByEmail("cliente@teste.com").orElse(null);
                    if (existente != null) {
                        return existente;
                    }
                    Cliente c = new Cliente();
                    c.setUser(user);
                    c.setNome("Cliente Teste");
                    c.setEmail("cliente@teste.com");
                    c.setTelefone("(11) 99999-9999");
                    Cliente saved = clienteRepository.save(c);
                    
                    // Criar endereço
                    EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
                    enderecoDTO.setLogradouro("Rua Teste");
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
                    Restaurante r = new Restaurante();
                    r.setNome("Restaurante Teste");
                    r.setEmail("restaurante@teste.com");
                    r.setTelefone("(11) 88888-8888");
                    User restauranteUser = testJwtUtil.getOrCreateUser("restaurante", ERole.ROLE_RESTAURANTE);
                    r.setUser(restauranteUser);
                    Restaurante saved = restauranteRepository.save(r);
                    
                    // Criar endereço
                    EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
                    enderecoDTO.setLogradouro("Rua Restaurante");
                    enderecoDTO.setNumero("456");
                    enderecoDTO.setBairro("Centro");
                    enderecoDTO.setCidade("São Paulo");
                    enderecoDTO.setEstado("SP");
                    enderecoDTO.setCep("01310100");
                    enderecoDTO.setPrincipal(true);
                    enderecoService.criarEndereco(enderecoDTO, saved);
                    
                    return saved;
                });
        
        restauranteToken = testJwtUtil.generateTokenForUser("restaurante", ERole.ROLE_RESTAURANTE);

        com.siseg.model.Endereco enderecoEntrega = enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não possui endereço"));

        pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setStatus(StatusPedido.CREATED);
        pedido.setMetodoPagamento(MetodoPagamento.PIX);
        pedido.setSubtotal(new BigDecimal("90.00"));
        pedido.setTaxaEntrega(new BigDecimal("10.00"));
        pedido.setTotal(new BigDecimal("100.00"));
        pedido.setEnderecoEntrega(enderecoEntrega);
        pedido = pedidoRepository.save(pedido);
    }

    @Test
    void deveCriarPagamentoParaPedido() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(cliente.getUser());

            PagamentoResponseDTO response = new PagamentoResponseDTO();
            response.setId(1L);
            response.setPedidoId(pedido.getId());
            response.setStatus(StatusPagamento.PENDING);
            response.setValor(new BigDecimal("100.00"));

            doReturn(response).when(pagamentoServiceClient).criarPagamento(any(Pedido.class), isNull(), anyString());

            mockMvc.perform(post("/api/pagamentos/pedidos/" + pedido.getId())
                            .header("Authorization", "Bearer " + clienteToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pedidoId").value(pedido.getId()))
                    .andExpect(jsonPath("$.status").exists());
        }
    }

    @Test
    void deveBuscarPagamentoPorPedido() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(cliente.getUser());

            PagamentoResponseDTO response = new PagamentoResponseDTO();
            response.setId(1L);
            response.setPedidoId(pedido.getId());
            response.setStatus(StatusPagamento.PENDING);
            response.setValor(new BigDecimal("100.00"));

            doReturn(response).when(pagamentoServiceClient).buscarPagamentoPorPedido(pedido.getId());

            mockMvc.perform(get("/api/pagamentos/pedidos/" + pedido.getId())
                            .header("Authorization", "Bearer " + clienteToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pedidoId").value(pedido.getId()));
        }
    }

    @Test
    void deveEstornarPagamentoComSucesso() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(cliente.getUser());

            PagamentoResponseDTO pagamentoResponse = new PagamentoResponseDTO();
            pagamentoResponse.setId(1L);
            pagamentoResponse.setPedidoId(pedido.getId());
            pagamentoResponse.setStatus(StatusPagamento.PAID);
            pagamentoResponse.setValor(new BigDecimal("100.00"));
            
            PagamentoResponseDTO reembolsoResponse = new PagamentoResponseDTO();
            reembolsoResponse.setId(1L);
            reembolsoResponse.setPedidoId(pedido.getId());
            reembolsoResponse.setStatus(StatusPagamento.REFUNDED);
            reembolsoResponse.setValor(new BigDecimal("100.00"));
            reembolsoResponse.setValorReembolsado(new BigDecimal("100.00"));

            doReturn(pagamentoResponse).when(pagamentoServiceClient).buscarPagamentoPorPedido(pedido.getId());
            doReturn(reembolsoResponse).when(pagamentoServiceClient).processarReembolso(eq(pedido.getId()), anyString());

            String requestBody = "{\"motivo\":\"Teste de reembolso\"}";

            mockMvc.perform(post("/api/pagamentos/pedidos/" + pedido.getId() + "/reembolso")
                            .header("Authorization", "Bearer " + restauranteToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUNDED"));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPagamentoJaReembolsado() throws Exception {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(cliente.getUser());

            PagamentoResponseDTO pagamentoResponse = new PagamentoResponseDTO();
            pagamentoResponse.setId(1L);
            pagamentoResponse.setPedidoId(pedido.getId());
            pagamentoResponse.setStatus(StatusPagamento.REFUNDED);
            pagamentoResponse.setValor(new BigDecimal("100.00"));

            doReturn(pagamentoResponse).when(pagamentoServiceClient).buscarPagamentoPorPedido(pedido.getId());

            String requestBody = "{\"motivo\":\"Teste de reembolso\"}";

            mockMvc.perform(post("/api/pagamentos/pedidos/" + pedido.getId() + "/reembolso")
                            .header("Authorization", "Bearer " + restauranteToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }
}

