package com.siseg.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.PagamentoService;
import com.siseg.service.PagamentoServiceClient;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PagamentoMonolitoMicroserviceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private TestJwtUtil testJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PagamentoServiceClient pagamentoServiceClient;

    private Cliente cliente;
    private Restaurante restaurante;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        User user = testJwtUtil.getOrCreateUser("cliente-test-integration", ERole.ROLE_CLIENTE);
        cliente = clienteRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setUser(user);
                    c.setNome("Cliente Teste Integration");
                    c.setEmail("cliente.integration@teste.com");
                    c.setTelefone("47999999999");
                    return clienteRepository.save(c);
                });
        
        restaurante = restauranteRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Restaurante r = new Restaurante();
                    r.setNome("Restaurante Teste Integration");
                    r.setEmail("restaurante.integration@teste.com");
                    r.setTelefone("47988888888");
                    return restauranteRepository.save(r);
                });
        
        pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setMetodoPagamento(MetodoPagamento.CREDIT_CARD);
        pedido.setTotal(new BigDecimal("50.00"));
        pedido.setStatus(StatusPedido.CREATED);
        pedido = pedidoRepository.save(pedido);
    }

    @Test
    void deveCriarPagamentoNoMicrosservicoEAtualizarPedido() {
        PagamentoResponseDTO mockResponse = new PagamentoResponseDTO();
        mockResponse.setId(1L);
        mockResponse.setPedidoId(pedido.getId());
        mockResponse.setValor(new BigDecimal("50.00"));
        mockResponse.setMetodo(MetodoPagamento.CREDIT_CARD);
        mockResponse.setStatus(StatusPagamento.AUTHORIZED);
        
        when(pagamentoServiceClient.criarPagamento(any(), any(), anyString()))
                .thenReturn(mockResponse);
        
    
        CartaoCreditoRequestDTO cartaoDTO = new CartaoCreditoRequestDTO();
        cartaoDTO.setNumero("5162306219378829");
        cartaoDTO.setNomeTitular("Cliente Teste");
        cartaoDTO.setValidade("12/25");
        cartaoDTO.setCvv("123");
        
        PagamentoResponseDTO response = pagamentoService.criarPagamento(
                pedido.getId(), 
                cartaoDTO, 
                "127.0.0.1"
        );
        
        assertNotNull(response);
        assertEquals(StatusPagamento.AUTHORIZED, response.getStatus());
        assertEquals(pedido.getId(), response.getPedidoId());
        
        Pedido pedidoAtualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.CONFIRMED, pedidoAtualizado.getStatus());
        
        verify(pagamentoServiceClient, times(1))
                .criarPagamento(any(Pedido.class), any(CartaoCreditoRequestDTO.class), eq("127.0.0.1"));
    }

    @Test
    void deveBuscarPagamentoNoMicrosservicoEAtualizarPedidoQuandoPAID() {
        PagamentoResponseDTO mockResponse = new PagamentoResponseDTO();
        mockResponse.setId(1L);
        mockResponse.setPedidoId(pedido.getId());
        mockResponse.setValor(new BigDecimal("50.00"));
        mockResponse.setMetodo(MetodoPagamento.PIX);
        mockResponse.setStatus(StatusPagamento.PAID);
        mockResponse.setQrCode("00020126360014BR.GOV.BCB.PIX...");
        
        when(pagamentoServiceClient.buscarPagamentoPorPedido(pedido.getId()))
                .thenReturn(mockResponse);
        
        PagamentoResponseDTO response = pagamentoService.buscarPagamentoPorPedido(pedido.getId());
        
        assertNotNull(response);
        assertEquals(StatusPagamento.PAID, response.getStatus());
        assertEquals(pedido.getId(), response.getPedidoId());
        
        Pedido pedidoAtualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.CONFIRMED, pedidoAtualizado.getStatus());
        
        verify(pagamentoServiceClient, times(1))
                .buscarPagamentoPorPedido(pedido.getId());
    }

    @Test
    void deveNotificarMonolitoQuandoPagamentoConfirmado() throws Exception {
        String serviceKey = "test-key-123";
        
        String notificacaoJson = objectMapper.writeValueAsString(
            java.util.Map.of(
                "pedidoId", pedido.getId(),
                "statusPagamento", "PAID",
                "asaasPaymentId", "pay_123456"
            )
        );
        
        mockMvc.perform(post("/api/pedidos/pagamento-confirmado")
                .header("X-Service-Key", serviceKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(notificacaoJson))
                .andExpect(status().isOk());
        
        Pedido pedidoAtualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.CONFIRMED, pedidoAtualizado.getStatus());
    }

    @Test
    void deveRejeitarNotificacaoComServiceKeyInvalida() throws Exception {
        String serviceKeyInvalida = "key-invalida";
        
        String notificacaoJson = objectMapper.writeValueAsString(
            java.util.Map.of(
                "pedidoId", pedido.getId(),
                "statusPagamento", "PAID",
                "asaasPaymentId", "pay_123456"
            )
        );
        
        mockMvc.perform(post("/api/pedidos/pagamento-confirmado")
                .header("X-Service-Key", serviceKeyInvalida)
                .contentType(MediaType.APPLICATION_JSON)
                .content(notificacaoJson))
                .andExpect(status().isForbidden());
        
        Pedido pedidoAtualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.CREATED, pedidoAtualizado.getStatus());
    }

    @Test
    void deveTratarErroQuandoMicrosservicoIndisponivel() {
        when(pagamentoServiceClient.criarPagamento(any(), any(), anyString()))
                .thenThrow(new RuntimeException("Erro de conexão com o serviço de pagamento"));
        
        CartaoCreditoRequestDTO cartaoDTO = new CartaoCreditoRequestDTO();
        cartaoDTO.setNumero("5162306219378829");
        cartaoDTO.setNomeTitular("Cliente Teste");
        cartaoDTO.setValidade("12/25");
        cartaoDTO.setCvv("123");
        
        assertThrows(RuntimeException.class, () -> {
            pagamentoService.criarPagamento(pedido.getId(), cartaoDTO, "127.0.0.1");
        });
        
        Pedido pedidoAtualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.CREATED, pedidoAtualizado.getStatus());
    }
}
