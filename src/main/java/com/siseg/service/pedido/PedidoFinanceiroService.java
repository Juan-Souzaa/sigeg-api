package com.siseg.service.pedido;

import com.siseg.dto.pedido.PedidoItemRequestDTO;
import com.siseg.dto.pagamento.PagamentoResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Carrinho;
import com.siseg.model.CarrinhoItem;
import com.siseg.model.Cupom;
import com.siseg.model.Pedido;
import com.siseg.model.PedidoItem;
import com.siseg.model.Prato;
import com.siseg.model.enumerations.StatusPagamento;
import com.siseg.model.enumerations.TipoDesconto;
import com.siseg.repository.PratoRepository;
import com.siseg.service.CarrinhoService;
import com.siseg.service.CupomService;
import com.siseg.service.PagamentoService;
import com.siseg.service.PagamentoServiceClient;
import com.siseg.service.TaxaCalculoService;
import com.siseg.util.CalculadoraFinanceira;
import com.siseg.validator.PedidoValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

@Service
public class PedidoFinanceiroService {

    private static final Logger logger = Logger.getLogger(PedidoFinanceiroService.class.getName());
    private static final BigDecimal VALOR_MINIMO_TAXA_ENTREGA = new BigDecimal("50.00");
    private static final BigDecimal TAXA_ENTREGA_PADRAO = new BigDecimal("5.00");

    private final PratoRepository pratoRepository;
    private final PedidoValidator pedidoValidator;
    private final CarrinhoService carrinhoService;
    private final CupomService cupomService;
    private final PagamentoService pagamentoService;
    private final PagamentoServiceClient pagamentoServiceClient;
    private final TaxaCalculoService taxaCalculoService;

    public PedidoFinanceiroService(PratoRepository pratoRepository,
                                   PedidoValidator pedidoValidator,
                                   CarrinhoService carrinhoService,
                                   CupomService cupomService,
                                   PagamentoService pagamentoService,
                                   PagamentoServiceClient pagamentoServiceClient,
                                   TaxaCalculoService taxaCalculoService) {
        this.pratoRepository = pratoRepository;
        this.pedidoValidator = pedidoValidator;
        this.carrinhoService = carrinhoService;
        this.cupomService = cupomService;
        this.pagamentoService = pagamentoService;
        this.pagamentoServiceClient = pagamentoServiceClient;
        this.taxaCalculoService = taxaCalculoService;
    }

    public void processarItensPedido(Pedido pedido, List<PedidoItemRequestDTO> itensDto) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (var itemDto : itensDto) {
            Prato prato = buscarPratoDisponivel(itemDto.getPratoId());
            PedidoItem item = criarPedidoItem(pedido, prato, itemDto);
            pedido.getItens().add(item);
            subtotal = subtotal.add(item.getSubtotal());
        }

        pedido.setSubtotal(subtotal);
    }

    public void calcularValoresPedido(Pedido pedido) {
        pedido.setTaxaEntrega(calcularTaxaEntrega(pedido.getSubtotal()));
        pedido.setTotal(pedido.getSubtotal().add(pedido.getTaxaEntrega()));
    }

    public void processarCarrinhoParaPedido(Pedido pedido, Long carrinhoId, Long clienteId) {
        Carrinho carrinho = validarECarregarCarrinho(carrinhoId, clienteId);
        BigDecimal subtotal = processarItensDoCarrinho(pedido, carrinho);
        aplicarCupomSeExistir(pedido, carrinho, subtotal);
    }

    public void processarReembolsoSeNecessario(Pedido pedido) {
        try {
            PagamentoResponseDTO pagamento = pagamentoServiceClient.buscarPagamentoPorPedido(pedido.getId());
            if (deveReembolsar(pagamento)) {
                String motivo = "Reembolso automático por cancelamento de pedido #" + pedido.getId();
                pagamentoService.processarReembolso(pedido.getId(), motivo);
                logger.info("Reembolso automático processado para pedido " + pedido.getId());
            }
        } catch (com.siseg.exception.ResourceNotFoundException e) {
            logger.fine("Pagamento não encontrado para pedido " + pedido.getId() + " - não será feito reembolso");
        } catch (Exception e) {
            logger.warning("Erro ao processar reembolso automático para pedido " + pedido.getId() + ": " + e.getMessage());
        }
    }

    public void calcularEAtualizarValoresPosEntrega(Pedido pedido) {
        taxaCalculoService.calcularEAtualizarValoresFinanceiros(pedido);
    }

    public void limparCarrinho() {
        carrinhoService.limparCarrinho();
    }

    private Carrinho validarECarregarCarrinho(Long carrinhoId, Long clienteId) {
        Carrinho carrinho = carrinhoService.obterCarrinhoParaPedido(clienteId);

        if (!carrinho.getId().equals(carrinhoId)) {
            throw new ResourceNotFoundException("Carrinho não encontrado ou não pertence ao cliente");
        }

        if (carrinho.getItens().isEmpty()) {
            throw new IllegalArgumentException("Carrinho está vazio");
        }

        return carrinho;
    }

    private BigDecimal processarItensDoCarrinho(Pedido pedido, Carrinho carrinho) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CarrinhoItem itemCarrinho : carrinho.getItens()) {
            Prato prato = pedidoValidator.validatePratoDisponivel(itemCarrinho.getPrato());
            PedidoItem item = criarPedidoItemDoCarrinho(pedido, prato, itemCarrinho);
            pedido.getItens().add(item);
            subtotal = subtotal.add(item.getSubtotal());
        }

        pedido.setSubtotal(subtotal);
        return subtotal;
    }

    private void aplicarCupomSeExistir(Pedido pedido, Carrinho carrinho, BigDecimal subtotal) {
        BigDecimal taxaEntrega = calcularTaxaEntrega(subtotal);
        pedido.setTaxaEntrega(taxaEntrega);

        if (carrinho.getCupom() != null) {
            BigDecimal desconto = calcularDescontoCupom(carrinho.getCupom(), subtotal);
            pedido.setTotal(subtotal.subtract(desconto).add(taxaEntrega));
            cupomService.incrementarUsoCupom(carrinho.getCupom());
        } else {
            pedido.setTotal(subtotal.add(taxaEntrega));
        }
    }

    private PedidoItem criarPedidoItem(Pedido pedido, Prato prato, PedidoItemRequestDTO itemDto) {
        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setPrato(prato);
        item.setQuantidade(itemDto.getQuantidade());
        item.setPrecoUnitario(prato.getPreco());
        item.setSubtotal(prato.getPreco().multiply(BigDecimal.valueOf(itemDto.getQuantidade())));
        return item;
    }

    private PedidoItem criarPedidoItemDoCarrinho(Pedido pedido, Prato prato, CarrinhoItem itemCarrinho) {
        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setPrato(prato);
        item.setQuantidade(itemCarrinho.getQuantidade());
        item.setPrecoUnitario(itemCarrinho.getPrecoUnitario());
        item.setSubtotal(itemCarrinho.getSubtotal());
        return item;
    }

    private Prato buscarPratoDisponivel(Long pratoId) {
        Prato prato = pratoRepository.findById(pratoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com ID: " + pratoId));

        return pedidoValidator.validatePratoDisponivel(prato);
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotal) {
        if (cupom.getTipoDesconto() == TipoDesconto.PERCENTUAL) {
            return CalculadoraFinanceira.calcularTaxaPlataforma(subtotal, cupom.getValorDesconto());
        }
        BigDecimal desconto = cupom.getValorDesconto();
        return desconto.compareTo(subtotal) > 0 ? subtotal : desconto;
    }

    private BigDecimal calcularTaxaEntrega(BigDecimal subtotal) {
        if (subtotal.compareTo(VALOR_MINIMO_TAXA_ENTREGA) >= 0) {
            return BigDecimal.ZERO;
        }
        return TAXA_ENTREGA_PADRAO;
    }

    private boolean deveReembolsar(PagamentoResponseDTO pagamento) {
        return pagamento.getStatus() == StatusPagamento.PAID ||
               pagamento.getStatus() == StatusPagamento.AUTHORIZED;
    }
}

