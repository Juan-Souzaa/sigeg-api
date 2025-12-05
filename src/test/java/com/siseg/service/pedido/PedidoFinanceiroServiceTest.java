package com.siseg.service.pedido;

import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.dto.pedido.PedidoItemRequestDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.*;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.TipoDesconto;
import com.siseg.repository.PratoRepository;
import com.siseg.service.CarrinhoService;
import com.siseg.service.CupomService;
import com.siseg.service.PagamentoService;
import com.siseg.service.PagamentoServiceClient;
import com.siseg.service.TaxaCalculoService;
import com.siseg.validator.PedidoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoFinanceiroServiceTest {

    @Mock
    private PratoRepository pratoRepository;

    @Mock
    private PedidoValidator pedidoValidator;

    @Mock
    private CarrinhoService carrinhoService;

    @Mock
    private CupomService cupomService;

    @Mock
    private PagamentoService pagamentoService;

    @Mock
    private PagamentoServiceClient pagamentoServiceClient;

    @Mock
    private TaxaCalculoService taxaCalculoService;

    @InjectMocks
    private PedidoFinanceiroService pedidoFinanceiroService;

    private Pedido pedido;
    private Prato prato;
    private PedidoItemRequestDTO itemDto;
    private Carrinho carrinho;
    private Cupom cupom;

    @BeforeEach
    void setUp() {
        pedido = new Pedido();
        pedido.setId(100L);
        pedido.setItens(new ArrayList<>());

        prato = new Prato();
        prato.setId(1L);
        prato.setPreco(new BigDecimal("25.00"));

        itemDto = new PedidoItemRequestDTO();
        itemDto.setPratoId(1L);
        itemDto.setQuantidade(2);

        carrinho = new Carrinho();
        carrinho.setId(55L);
        carrinho.setItens(new ArrayList<>());

        carrinho.setCupom(null);

        cupom = new Cupom();
        cupom.setId(9L);
        cupom.setTipoDesconto(TipoDesconto.VALOR_FIXO);
        cupom.setValorDesconto(new BigDecimal("5.00"));
    }

    @Test
    void deveProcessarItensDiretosDoPedido() {
        when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
        when(pedidoValidator.validatePratoDisponivel(prato)).thenReturn(prato);

        pedidoFinanceiroService.processarItensPedido(pedido, List.of(itemDto));

        assertEquals(new BigDecimal("50.00"), pedido.getSubtotal());
        assertEquals(1, pedido.getItens().size());
        PedidoItem itemSalvo = pedido.getItens().get(0);
        assertEquals(2, itemSalvo.getQuantidade());
        assertEquals(prato, itemSalvo.getPrato());
    }

    @Test
    void deveProcessarCarrinhoAplicandoCupom() {
        CarrinhoItem carrinhoItem = new CarrinhoItem();
        carrinhoItem.setPrato(prato);
        carrinhoItem.setQuantidade(2);
        carrinhoItem.setPrecoUnitario(prato.getPreco());
        carrinhoItem.setSubtotal(new BigDecimal("50.00"));
        carrinho.getItens().add(carrinhoItem);
        carrinho.setCupom(cupom);

        when(carrinhoService.obterCarrinhoParaPedido(10L)).thenReturn(carrinho);
        when(pedidoValidator.validatePratoDisponivel(prato)).thenReturn(prato);

        pedidoFinanceiroService.processarCarrinhoParaPedido(pedido, 55L, 10L);

        assertEquals(new BigDecimal("50.00"), pedido.getSubtotal());
        assertEquals(BigDecimal.ZERO, pedido.getTaxaEntrega()); // subtotal >= 50
        assertEquals(new BigDecimal("45.00"), pedido.getTotal());
        verify(cupomService).incrementarUsoCupom(cupom);
    }

    @Test
    void deveFalharQuandoCarrinhoNaoPertenceAoCliente() {
        when(carrinhoService.obterCarrinhoParaPedido(10L)).thenReturn(carrinho);

        assertThrows(ResourceNotFoundException.class,
            () -> pedidoFinanceiroService.processarCarrinhoParaPedido(pedido, 999L, 10L));
    }

    @Test
    void deveProcessarReembolsoQuandoPagamentoElegivel() {
        PagamentoResponseDTO pagamentoResponse = new PagamentoResponseDTO();
        pagamentoResponse.setStatus(StatusPagamento.PAID);

        when(pagamentoServiceClient.buscarPagamentoPorPedido(pedido.getId())).thenReturn(pagamentoResponse);
        doReturn(new PagamentoResponseDTO()).when(pagamentoService).processarReembolso(eq(pedido.getId()), anyString());

        pedidoFinanceiroService.processarReembolsoSeNecessario(pedido);

        verify(pagamentoService).processarReembolso(eq(pedido.getId()), contains("#" + pedido.getId()));
    }

    @Test
    void naoDeveProcessarReembolsoQuandoPagamentoNaoElegivel() {
        PagamentoResponseDTO pagamentoResponse = new PagamentoResponseDTO();
        pagamentoResponse.setStatus(StatusPagamento.PENDING);

        when(pagamentoServiceClient.buscarPagamentoPorPedido(pedido.getId())).thenReturn(pagamentoResponse);

        pedidoFinanceiroService.processarReembolsoSeNecessario(pedido);

        verify(pagamentoService, never()).processarReembolso(anyLong(), anyString());
    }

    @Test
    void naoDeveProcessarReembolsoQuandoNaoHaPagamento() {
        when(pagamentoServiceClient.buscarPagamentoPorPedido(pedido.getId()))
                .thenThrow(new ResourceNotFoundException("Pagamento não encontrado"));

        pedidoFinanceiroService.processarReembolsoSeNecessario(pedido);

        verify(pagamentoService, never()).processarReembolso(anyLong(), anyString());
    }

    @Test
    void deveCalcularValoresPedidoComTaxaEntrega() {
        pedido.setSubtotal(new BigDecimal("30.00"));

        pedidoFinanceiroService.calcularValoresPedido(pedido);

        // Subtotal < 50, então deve ter taxa de entrega de 5.00
        assertEquals(new BigDecimal("5.00"), pedido.getTaxaEntrega());
        assertEquals(new BigDecimal("35.00"), pedido.getTotal());
    }

    @Test
    void deveCalcularValoresPedidoSemTaxaEntrega() {
        pedido.setSubtotal(new BigDecimal("60.00"));

        pedidoFinanceiroService.calcularValoresPedido(pedido);

        // Subtotal >= 50, taxa de entrega = 0
        assertEquals(BigDecimal.ZERO, pedido.getTaxaEntrega());
        assertEquals(new BigDecimal("60.00"), pedido.getTotal());
    }

    @Test
    void deveProcessarCarrinhoSemCupom() {
        CarrinhoItem carrinhoItem = new CarrinhoItem();
        carrinhoItem.setPrato(prato);
        carrinhoItem.setQuantidade(2);
        carrinhoItem.setPrecoUnitario(prato.getPreco());
        carrinhoItem.setSubtotal(new BigDecimal("50.00"));
        carrinho.getItens().add(carrinhoItem);

        when(carrinhoService.obterCarrinhoParaPedido(10L)).thenReturn(carrinho);
        when(pedidoValidator.validatePratoDisponivel(prato)).thenReturn(prato);

        pedidoFinanceiroService.processarCarrinhoParaPedido(pedido, 55L, 10L);

        assertEquals(new BigDecimal("50.00"), pedido.getSubtotal());
        assertEquals(BigDecimal.ZERO, pedido.getTaxaEntrega());
        assertEquals(new BigDecimal("50.00"), pedido.getTotal());
        verify(cupomService, never()).incrementarUsoCupom(any());
    }

    @Test
    void deveProcessarCarrinhoComCupomPercentual() {
        CarrinhoItem carrinhoItem = new CarrinhoItem();
        carrinhoItem.setPrato(prato);
        carrinhoItem.setQuantidade(2);
        carrinhoItem.setPrecoUnitario(prato.getPreco());
        carrinhoItem.setSubtotal(new BigDecimal("50.00"));
        carrinho.getItens().add(carrinhoItem);
        
        Cupom cupomPercentual = new Cupom();
        cupomPercentual.setId(9L);
        cupomPercentual.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupomPercentual.setValorDesconto(new BigDecimal("10.00")); // 10%
        carrinho.setCupom(cupomPercentual);

        when(carrinhoService.obterCarrinhoParaPedido(10L)).thenReturn(carrinho);
        when(pedidoValidator.validatePratoDisponivel(prato)).thenReturn(prato);

        pedidoFinanceiroService.processarCarrinhoParaPedido(pedido, 55L, 10L);

        assertEquals(new BigDecimal("50.00"), pedido.getSubtotal());
        // Total = 50 - (50 * 0.10) + 0 = 45.00
        assertEquals(new BigDecimal("45.00"), pedido.getTotal());
        verify(cupomService).incrementarUsoCupom(cupomPercentual);
    }

    @Test
    void deveLimparCarrinho() {
        doNothing().when(carrinhoService).limparCarrinho();

        pedidoFinanceiroService.limparCarrinho();

        verify(carrinhoService).limparCarrinho();
    }

    @Test
    void deveCalcularEAtualizarValoresPosEntrega() {
        doNothing().when(taxaCalculoService).calcularEAtualizarValoresFinanceiros(pedido);

        pedidoFinanceiroService.calcularEAtualizarValoresPosEntrega(pedido);

        verify(taxaCalculoService).calcularEAtualizarValoresFinanceiros(pedido);
    }
}


