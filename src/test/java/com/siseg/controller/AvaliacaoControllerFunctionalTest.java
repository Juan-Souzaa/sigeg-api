package com.siseg.controller;

import com.siseg.dto.avaliacao.AvaliacaoRequestDTO;
import com.siseg.dto.avaliacao.AvaliacaoResponseDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.service.AvaliacaoService;
import com.siseg.util.TestJwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;  
import com.siseg.model.enumerations.ERole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvaliacaoControllerFunctionalTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private TestJwtUtil testJwtUtil;
    
    @MockBean
    private AvaliacaoService avaliacaoService;
    
    @MockBean
    private PedidoRepository pedidoRepository;
    
    @MockBean
    private ClienteRepository clienteRepository;
    
    @MockBean
    private AvaliacaoRepository avaliacaoRepository;
    
    private String clienteToken;
    private Pedido pedido;
    private AvaliacaoRequestDTO requestDTO;
    
    @BeforeEach
    void setUp() throws Exception {
        try {
            clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
        } catch (Exception e) {
            try {
                Thread.sleep(100);
                clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
            } catch (Exception ex) {
                throw new RuntimeException("Erro ao gerar token JWT para testes", ex);
            }
        }
        
        User clienteUser = new User();
        clienteUser.setId(1L);
        
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setUser(clienteUser);
        
        Restaurante restaurante = new Restaurante();
        restaurante.setId(1L);
        
        Entregador entregador = new Entregador();
        entregador.setId(1L);
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);
        
        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setEntregador(entregador);
        pedido.setStatus(StatusPedido.DELIVERED);
        
        requestDTO = new AvaliacaoRequestDTO();
        requestDTO.setNotaRestaurante(5);
        requestDTO.setNotaEntregador(4);
        requestDTO.setNotaPedido(5);
        requestDTO.setComentarioRestaurante("Ótimo restaurante!");
        
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
    }
    
    @Test
    void deveCriarAvaliacaoComSucesso() throws Exception {
        AvaliacaoResponseDTO responseDTO = new AvaliacaoResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setNotaRestaurante(5);
        responseDTO.setNotaPedido(5);
        
        when(avaliacaoService.criarAvaliacao(eq(1L), any(AvaliacaoRequestDTO.class))).thenReturn(responseDTO);
        
        String json = """
            {
                "notaRestaurante": 5,
                "notaEntregador": 4,
                "notaPedido": 5,
                "comentarioRestaurante": "Ótimo restaurante!"
            }
            """;
        
        mockMvc.perform(post("/api/avaliacoes/pedidos/1")
                .header("Authorization", "Bearer " + clienteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.notaRestaurante").value(5))
                .andExpect(jsonPath("$.notaPedido").value(5));
    }
    
    @Test
    void deveEditarAvaliacaoComSucesso() throws Exception {
        AvaliacaoResponseDTO responseDTO = new AvaliacaoResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setNotaRestaurante(4);
        
        when(avaliacaoService.editarAvaliacao(eq(1L), any(AvaliacaoRequestDTO.class))).thenReturn(responseDTO);
        
        String json = """
            {
                "notaRestaurante": 4,
                "notaPedido": 5
            }
            """;
        
        mockMvc.perform(put("/api/avaliacoes/1")
                .header("Authorization", "Bearer " + clienteToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.notaRestaurante").value(4));
    }
    
    @Test
    void deveListarAvaliacoesPorRestaurante() throws Exception {
        AvaliacaoResponseDTO avaliacao1 = new AvaliacaoResponseDTO();
        avaliacao1.setId(1L);
        avaliacao1.setNotaRestaurante(5);
        
        AvaliacaoResponseDTO avaliacao2 = new AvaliacaoResponseDTO();
        avaliacao2.setId(2L);
        avaliacao2.setNotaRestaurante(4);
        
        Page<AvaliacaoResponseDTO> page = new PageImpl<>(Arrays.asList(avaliacao1, avaliacao2), PageRequest.of(0, 10), 2);
        
        when(avaliacaoService.listarAvaliacoesPorRestaurante(eq(1L), any())).thenReturn(page);
        
        mockMvc.perform(get("/api/avaliacoes/restaurantes/1")
                .header("Authorization", "Bearer " + clienteToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}

