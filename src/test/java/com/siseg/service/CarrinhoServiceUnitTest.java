package com.siseg.service;

import com.siseg.dto.carrinho.AplicarCupomRequestDTO;
import com.siseg.dto.carrinho.CarrinhoItemRequestDTO;
import com.siseg.dto.carrinho.CarrinhoResponseDTO;
import com.siseg.exception.PratoNotAvailableException;
import com.siseg.model.Carrinho;
import com.siseg.model.CarrinhoItem;
import com.siseg.model.Cliente;
import com.siseg.model.Cupom;
import com.siseg.model.Prato;
import com.siseg.model.User;
import com.siseg.model.enumerations.TipoDesconto;
import com.siseg.mapper.CarrinhoMapper;
import com.siseg.repository.CarrinhoRepository;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PratoRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.CupomValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrinhoServiceUnitTest {

    @Mock
    private CarrinhoRepository carrinhoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PratoRepository pratoRepository;

    @Mock
    private CupomService cupomService;

    @Mock
    private CupomValidator cupomValidator;

    @Mock
    private CarrinhoMapper carrinhoMapper;

    @InjectMocks
    private CarrinhoService carrinhoService;

    private User user;
    private Cliente cliente;
    private Carrinho carrinho;
    private Prato prato;
    private CarrinhoItemRequestDTO itemRequestDTO;
    private CarrinhoResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setUser(user);

        carrinho = new Carrinho();
        carrinho.setId(1L);
        carrinho.setCliente(cliente);
        carrinho.setItens(new ArrayList<>());
        carrinho.setSubtotal(BigDecimal.ZERO);
        carrinho.setDesconto(BigDecimal.ZERO);
        carrinho.setTotal(BigDecimal.ZERO);

        prato = new Prato();
        prato.setId(1L);
        prato.setNome("Prato Teste");
        prato.setPreco(new BigDecimal("25.00"));
        prato.setDisponivel(true);

        itemRequestDTO = new CarrinhoItemRequestDTO();
        itemRequestDTO.setPratoId(1L);
        itemRequestDTO.setQuantidade(2);

        responseDTO = new CarrinhoResponseDTO();
        responseDTO.setId(1L);
    }

    @Test
    void deveAdicionarItemAoCarrinhoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
            when(carrinhoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrinho));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(carrinhoRepository.save(any(Carrinho.class))).thenReturn(carrinho);
            when(carrinhoMapper.toResponseDTO(carrinho)).thenReturn(responseDTO);

            CarrinhoResponseDTO resultado = carrinhoService.adicionarItem(itemRequestDTO);

            assertNotNull(resultado);
            verify(pratoRepository).findById(1L);
            verify(carrinhoRepository).save(any(Carrinho.class));
        }
    }

    @Test
    void deveLancarExcecaoQuandoPratoNaoDisponivel() {
        prato.setDisponivel(false);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
            when(carrinhoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrinho));
            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));

            assertThrows(PratoNotAvailableException.class, () -> 
                    carrinhoService.adicionarItem(itemRequestDTO));
        }
    }

    @Test
    void deveRemoverItemDoCarrinhoComSucesso() {
        CarrinhoItem item = new CarrinhoItem();
        item.setId(1L);
        item.setCarrinho(carrinho);
        item.setPrato(prato);
        carrinho.getItens().add(item);

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
            when(carrinhoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrinho));
            when(carrinhoRepository.save(any(Carrinho.class))).thenReturn(carrinho);
            when(carrinhoMapper.toResponseDTO(carrinho)).thenReturn(responseDTO);

            CarrinhoResponseDTO resultado = carrinhoService.removerItem(1L);

            assertNotNull(resultado);
            assertTrue(carrinho.getItens().isEmpty());
            verify(carrinhoRepository).save(any(Carrinho.class));
        }
    }

    @Test
    void deveAplicarCupomComSucesso() {
        Cupom cupom = new Cupom();
        cupom.setId(1L);
        cupom.setCodigo("DESCONTO10");
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValorDesconto(new BigDecimal("10.00"));
        cupom.setValorMinimo(new BigDecimal("50.00"));
        cupom.setDataInicio(LocalDate.now().minusDays(5));
        cupom.setDataFim(LocalDate.now().plusDays(25));
        cupom.setAtivo(true);

        carrinho.setSubtotal(new BigDecimal("100.00"));

        AplicarCupomRequestDTO aplicarCupomDTO = new AplicarCupomRequestDTO();
        aplicarCupomDTO.setCodigo("DESCONTO10");

        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
            when(carrinhoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrinho));
            when(cupomService.buscarPorCodigo("DESCONTO10")).thenReturn(cupom);
            doNothing().when(cupomValidator).validateCupomAplicavel(cupom, new BigDecimal("100.00"));
            when(carrinhoRepository.save(any(Carrinho.class))).thenReturn(carrinho);
            when(carrinhoMapper.toResponseDTO(carrinho)).thenReturn(responseDTO);

            CarrinhoResponseDTO resultado = carrinhoService.aplicarCupom(aplicarCupomDTO);

            assertNotNull(resultado);
            assertEquals(cupom, carrinho.getCupom());
            verify(cupomService).buscarPorCodigo(aplicarCupomDTO.getCodigo());
            verify(cupomValidator).validateCupomAplicavel(cupom, new BigDecimal("100.00"));
        }
    }

    @Test
    void deveCriarCarrinhoQuandoNaoExiste() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::getCurrentUser).thenReturn(user);
            when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
            when(carrinhoRepository.findByClienteId(1L)).thenReturn(Optional.empty());
            when(carrinhoRepository.save(any(Carrinho.class))).thenReturn(carrinho);
            when(carrinhoMapper.toResponseDTO(carrinho)).thenReturn(responseDTO);

            CarrinhoResponseDTO resultado = carrinhoService.obterCarrinhoAtivo();

            assertNotNull(resultado);
            verify(carrinhoRepository).save(any(Carrinho.class));
        }
    }
}

