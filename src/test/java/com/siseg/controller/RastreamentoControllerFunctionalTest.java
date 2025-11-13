package com.siseg.controller;

import com.siseg.dto.rastreamento.RastreamentoDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.service.RastreamentoService;
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


import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RastreamentoControllerFunctionalTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private TestJwtUtil testJwtUtil;
    
    @MockBean
    private PedidoRepository pedidoRepository;
    
    @MockBean
    private RastreamentoService rastreamentoService;
    
    @MockBean
    private ClienteRepository clienteRepository;
    
    private String clienteToken;
    private Pedido pedido;
    private Cliente cliente;
    
    @BeforeEach
    void setUp() throws Exception {
        clienteToken = testJwtUtil.generateTokenForUser("testcliente", ERole.ROLE_CLIENTE);
        
        User clienteUser = testJwtUtil.getOrCreateUser("testcliente", ERole.ROLE_CLIENTE);
        
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setUser(clienteUser);
        
        when(clienteRepository.findByUserId(clienteUser.getId())).thenReturn(Optional.of(cliente));
        
        Entregador entregador = new Entregador();
        entregador.setId(1L);
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);
        entregador.setLatitude(new BigDecimal("-23.5505"));
        entregador.setLongitude(new BigDecimal("-46.6333"));
        
        Restaurante restaurante = new Restaurante();
        restaurante.setId(1L);
        
        // Criar endereço de entrega
        Endereco enderecoEntrega = new Endereco();
        enderecoEntrega.setId(1L);
        enderecoEntrega.setLogradouro("Rua de Entrega");
        enderecoEntrega.setNumero("456");
        enderecoEntrega.setBairro("Centro");
        enderecoEntrega.setCidade("São Paulo");
        enderecoEntrega.setEstado("SP");
        enderecoEntrega.setCep("01310100");
        enderecoEntrega.setLatitude(new BigDecimal("-23.5631"));
        enderecoEntrega.setLongitude(new BigDecimal("-46.6542"));
        enderecoEntrega.setPrincipal(false);
        enderecoEntrega.setTipo(TipoEndereco.OUTRO);
        
        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setEntregador(entregador);
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        pedido.setEnderecoEntrega(enderecoEntrega);
    }
    
    @Test
    void deveConsultarRastreamentoComSucesso() throws Exception {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        
        RastreamentoDTO rastreamento = new RastreamentoDTO();
        rastreamento.setPosicaoAtualLat(new BigDecimal("-23.5505"));
        rastreamento.setPosicaoAtualLon(new BigDecimal("-46.6333"));
        rastreamento.setPosicaoDestinoLat(new BigDecimal("-23.5631"));
        rastreamento.setPosicaoDestinoLon(new BigDecimal("-46.6542"));
        rastreamento.setDistanciaRestanteKm(new BigDecimal("1.5"));
        rastreamento.setTempoEstimadoMinutos(15);
        rastreamento.setStatusEntrega(StatusPedido.OUT_FOR_DELIVERY);
        rastreamento.setProximoAoDestino(false);
        
        when(rastreamentoService.obterRastreamento(1L)).thenReturn(rastreamento);
        
        mockMvc.perform(get("/api/pedidos/1/rastreamento")
                .header("Authorization", "Bearer " + clienteToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicaoAtualLat").value(-23.5505))
                .andExpect(jsonPath("$.posicaoAtualLon").value(-46.6333))
                .andExpect(jsonPath("$.posicaoDestinoLat").value(-23.5631))
                .andExpect(jsonPath("$.posicaoDestinoLon").value(-46.6542))
                .andExpect(jsonPath("$.distanciaRestanteKm").value(1.5))
                .andExpect(jsonPath("$.tempoEstimadoMinutos").value(15))
                .andExpect(jsonPath("$.statusEntrega").value("OUT_FOR_DELIVERY"))
                .andExpect(jsonPath("$.proximoAoDestino").value(false));
    }
    
    @Test
    void deveRetornar404QuandoPedidoNaoExiste() throws Exception {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/api/pedidos/1/rastreamento")
                .header("Authorization", "Bearer " + clienteToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

