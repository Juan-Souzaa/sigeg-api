package com.siseg.service;

import com.siseg.dto.avaliacao.AvaliacaoRequestDTO;
import com.siseg.dto.avaliacao.AvaliacaoResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.AvaliacaoAlreadyExistsException;
import com.siseg.model.Avaliacao;
import com.siseg.model.Cliente;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.mapper.AvaliacaoMapper;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.AvaliacaoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceUnitTest {
    
    @Mock
    private AvaliacaoRepository avaliacaoRepository;
    
    @Mock
    private PedidoRepository pedidoRepository;
    
    @Mock
    private ClienteRepository clienteRepository;
    
    @Mock
    private ModelMapper modelMapper;

    @Mock
    private AvaliacaoMapper avaliacaoMapper;

    @Mock
    private AvaliacaoValidator avaliacaoValidator;
    
    @InjectMocks
    private AvaliacaoService avaliacaoService;
    
    private Pedido pedido;
    private Cliente cliente;
    private Restaurante restaurante;
    private Entregador entregador;
    private User user;
    private AvaliacaoRequestDTO requestDTO;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setUser(user);
        
        restaurante = new Restaurante();
        restaurante.setId(1L);
        
        entregador = new Entregador();
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
        requestDTO.setComentarioEntregador("Entrega rápida");
        requestDTO.setComentarioPedido("Pedido perfeito");
    }
    
    @Test
    void deveCriarAvaliacaoComSucesso() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(user);
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
            
            doNothing().when(avaliacaoValidator).validatePermissaoAvaliacao(any(Pedido.class));
            doNothing().when(avaliacaoValidator).validatePedidoEntregue(any(Pedido.class));
            doNothing().when(avaliacaoValidator).validateAvaliacaoNaoExistente(anyLong(), anyLong());
            
            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setId(1L);
            avaliacao.setPedido(pedido);
            avaliacao.setCliente(cliente);
            avaliacao.setRestaurante(restaurante);
            avaliacao.setEntregador(entregador);
            avaliacao.setNotaRestaurante(5);
            avaliacao.setNotaEntregador(4);
            avaliacao.setNotaPedido(5);
            
            when(avaliacaoRepository.save(any(Avaliacao.class))).thenReturn(avaliacao);
            
            AvaliacaoResponseDTO responseDTO = new AvaliacaoResponseDTO();
            responseDTO.setId(1L);
            when(avaliacaoMapper.toResponseDTO(any(Avaliacao.class))).thenReturn(responseDTO);
            
            AvaliacaoResponseDTO result = avaliacaoService.criarAvaliacao(1L, requestDTO);
            
            assertNotNull(result);
            verify(avaliacaoRepository, times(1)).save(any(Avaliacao.class));
        }
    }
    
    @Test
    void deveLancarExcecaoQuandoPedidoNaoEstaDelivered() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(user);
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            
            pedido.setStatus(StatusPedido.PREPARING);
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
            
            doNothing().when(avaliacaoValidator).validatePermissaoAvaliacao(any(Pedido.class));
            doThrow(new IllegalStateException("Pedido não está entregue"))
                    .when(avaliacaoValidator).validatePedidoEntregue(any(Pedido.class));
            
            assertThrows(IllegalStateException.class, () -> {
                avaliacaoService.criarAvaliacao(1L, requestDTO);
            });
        }
    }
    
    @Test
    void deveLancarExcecaoQuandoAvaliacaoJaExiste() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(user);
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
            when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
            
            doNothing().when(avaliacaoValidator).validatePermissaoAvaliacao(any(Pedido.class));
            doNothing().when(avaliacaoValidator).validatePedidoEntregue(any(Pedido.class));
            doThrow(new AvaliacaoAlreadyExistsException("Avaliação já existe para este pedido"))
                    .when(avaliacaoValidator).validateAvaliacaoNaoExistente(anyLong(), anyLong());
            
            assertThrows(AvaliacaoAlreadyExistsException.class, () -> {
                avaliacaoService.criarAvaliacao(1L, requestDTO);
            });
        }
    }
    
    @Test
    void deveEditarAvaliacaoComSucesso() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(user);
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            
            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setId(1L);
            avaliacao.setCliente(cliente);
            avaliacao.setPedido(pedido);
            avaliacao.setRestaurante(restaurante);
            avaliacao.setNotaRestaurante(3);
            
            when(avaliacaoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
            when(avaliacaoRepository.save(any(Avaliacao.class))).thenReturn(avaliacao);
            
            doNothing().when(avaliacaoValidator).validateOwnership(any(Avaliacao.class), any(User.class));
            
            AvaliacaoResponseDTO responseDTO = new AvaliacaoResponseDTO();
            when(avaliacaoMapper.toResponseDTO(any(Avaliacao.class))).thenReturn(responseDTO);
            
            AvaliacaoRequestDTO editDTO = new AvaliacaoRequestDTO();
            editDTO.setNotaRestaurante(5);
            editDTO.setNotaPedido(5);
            
            AvaliacaoResponseDTO result = avaliacaoService.editarAvaliacao(1L, editDTO);
            
            assertNotNull(result);
            verify(avaliacaoRepository, times(1)).save(any(Avaliacao.class));
            assertEquals(5, avaliacao.getNotaRestaurante());
        }
    }
    
    @Test
    void deveLancarExcecaoAoEditarAvaliacaoDeOutroCliente() {
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            User outroUser = new User();
            outroUser.setId(2L);
            
            Cliente outroCliente = new Cliente();
            outroCliente.setId(2L);
            outroCliente.setUser(outroUser);
            
            securityUtilsMock.when(SecurityUtils::getCurrentUser).thenReturn(user);
            securityUtilsMock.when(SecurityUtils::isAdmin).thenReturn(false);
            
            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setId(1L);
            avaliacao.setCliente(outroCliente);
            
            when(avaliacaoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
            
            doThrow(new AccessDeniedException("Você não tem permissão para editar esta avaliação"))
                    .when(avaliacaoValidator).validateOwnership(any(Avaliacao.class), any(User.class));
            
            assertThrows(AccessDeniedException.class, () -> {
                avaliacaoService.editarAvaliacao(1L, requestDTO);
            });
        }
    }
    
    @Test
    void deveCalcularMediaRestaurante() {
        when(avaliacaoRepository.calcularMediaNotaRestaurante(1L)).thenReturn(new BigDecimal("4.5"));
        
        BigDecimal media = avaliacaoService.calcularMediaRestaurante(1L);
        
        assertNotNull(media);
        assertEquals(0, new BigDecimal("4.50").compareTo(media));
    }
    
    @Test
    void deveContarAvaliacoesRestaurante() {
        when(avaliacaoRepository.countByRestauranteId(1L)).thenReturn(10L);
        
        long total = avaliacaoService.contarAvaliacoesRestaurante(1L);
        
        assertEquals(10L, total);
    }
}

