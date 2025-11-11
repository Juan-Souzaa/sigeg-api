package com.siseg.service;

import com.siseg.dto.carrinho.AplicarCupomRequestDTO;
import com.siseg.dto.carrinho.CarrinhoItemRequestDTO;
import com.siseg.dto.carrinho.CarrinhoResponseDTO;
import com.siseg.exception.PratoNotAvailableException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Carrinho;
import com.siseg.model.CarrinhoItem;
import com.siseg.model.Cliente;
import com.siseg.model.Cupom;
import com.siseg.model.Prato;
import com.siseg.model.User;
import com.siseg.mapper.CarrinhoMapper;
import com.siseg.repository.CarrinhoRepository;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PratoRepository;
import com.siseg.util.CalculadoraFinanceira;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.CupomValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
public class CarrinhoService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final CarrinhoRepository carrinhoRepository;
    private final ClienteRepository clienteRepository;
    private final PratoRepository pratoRepository;
    private final CupomService cupomService;
    private final CupomValidator cupomValidator;
    private final CarrinhoMapper carrinhoMapper;

    public CarrinhoService(CarrinhoRepository carrinhoRepository, ClienteRepository clienteRepository,
                           PratoRepository pratoRepository, CupomService cupomService,
                           CupomValidator cupomValidator, CarrinhoMapper carrinhoMapper) {
        this.carrinhoRepository = carrinhoRepository;
        this.clienteRepository = clienteRepository;
        this.pratoRepository = pratoRepository;
        this.cupomService = cupomService;
        this.cupomValidator = cupomValidator;
        this.carrinhoMapper = carrinhoMapper;
    }

    @Transactional(readOnly = true)
    public CarrinhoResponseDTO obterCarrinhoAtivo() {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        Carrinho carrinho = obterOuCriarCarrinho(cliente);
        return carrinhoMapper.toResponseDTO(carrinho);
    }

    public CarrinhoResponseDTO adicionarItem(CarrinhoItemRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        Carrinho carrinho = obterOuCriarCarrinho(cliente);
        
        Prato prato = buscarPrato(dto.getPratoId());
        validarPratoDisponivel(prato);
        
        adicionarItemAoCarrinho(carrinho, prato, dto.getQuantidade());
        recalcularValores(carrinho);
        
        Carrinho saved = carrinhoRepository.save(carrinho);
        return carrinhoMapper.toResponseDTO(saved);
    }

    public CarrinhoResponseDTO removerItem(Long itemId) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        Carrinho carrinho = obterOuCriarCarrinho(cliente);
        
        CarrinhoItem item = buscarItemNoCarrinho(carrinho, itemId);
        carrinho.getItens().remove(item);
        
        recalcularValores(carrinho);
        Carrinho saved = carrinhoRepository.save(carrinho);
        return carrinhoMapper.toResponseDTO(saved);
    }

    public CarrinhoResponseDTO atualizarQuantidade(Long itemId, Integer quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        Carrinho carrinho = obterOuCriarCarrinho(cliente);
        
        CarrinhoItem item = buscarItemNoCarrinho(carrinho, itemId);
        item.setQuantidade(quantidade);
        item.setSubtotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidade))
                .setScale(SCALE, ROUNDING_MODE));
        
        recalcularValores(carrinho);
        Carrinho saved = carrinhoRepository.save(carrinho);
        return carrinhoMapper.toResponseDTO(saved);
    }

    public CarrinhoResponseDTO aplicarCupom(AplicarCupomRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        Carrinho carrinho = obterOuCriarCarrinho(cliente);
        
        Cupom cupom = cupomService.buscarPorCodigo(dto.getCodigo());
        cupomValidator.validateCupomAplicavel(cupom, carrinho.getSubtotal());
        
        carrinho.setCupom(cupom);
        recalcularValores(carrinho);
        
        Carrinho saved = carrinhoRepository.save(carrinho);
        return carrinhoMapper.toResponseDTO(saved);
    }

    public CarrinhoResponseDTO removerCupom() {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        Carrinho carrinho = obterOuCriarCarrinho(cliente);
        
        carrinho.setCupom(null);
        recalcularValores(carrinho);
        
        Carrinho saved = carrinhoRepository.save(carrinho);
        return carrinhoMapper.toResponseDTO(saved);
    }

    public void limparCarrinho() {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        Carrinho carrinho = obterOuCriarCarrinho(cliente);
        
        carrinho.getItens().clear();
        carrinho.setCupom(null);
        recalcularValores(carrinho);
        carrinhoRepository.save(carrinho);
    }

    @Transactional(readOnly = true)
    public Carrinho obterCarrinhoParaPedido(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));
        return obterOuCriarCarrinho(cliente);
    }

    private Cliente buscarClientePorUsuario(User currentUser) {
        return clienteRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário autenticado"));
    }

    private Carrinho obterOuCriarCarrinho(Cliente cliente) {
        return carrinhoRepository.findByClienteId(cliente.getId())
                .orElseGet(() -> criarCarrinhoVazio(cliente));
    }

    private Carrinho criarCarrinhoVazio(Cliente cliente) {
        Carrinho carrinho = new Carrinho();
        carrinho.setCliente(cliente);
        carrinho.setSubtotal(BigDecimal.ZERO);
        carrinho.setDesconto(BigDecimal.ZERO);
        carrinho.setTotal(BigDecimal.ZERO);
        return carrinhoRepository.save(carrinho);
    }

    private Prato buscarPrato(Long pratoId) {
        return pratoRepository.findById(pratoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com ID: " + pratoId));
    }

    private void validarPratoDisponivel(Prato prato) {
        if (!prato.getDisponivel()) {
            throw new PratoNotAvailableException("Prato não está disponível");
        }
    }

    private void adicionarItemAoCarrinho(Carrinho carrinho, Prato prato, Integer quantidade) {
        CarrinhoItem itemExistente = carrinho.getItens().stream()
                .filter(item -> item.getPrato().getId().equals(prato.getId()))
                .findFirst()
                .orElse(null);

        if (itemExistente != null) {
            itemExistente.setQuantidade(itemExistente.getQuantidade() + quantidade);
            itemExistente.setSubtotal(itemExistente.getPrecoUnitario()
                    .multiply(BigDecimal.valueOf(itemExistente.getQuantidade()))
                    .setScale(SCALE, ROUNDING_MODE));
        } else {
            CarrinhoItem novoItem = criarItemCarrinho(carrinho, prato, quantidade);
            carrinho.getItens().add(novoItem);
        }
    }

    private CarrinhoItem criarItemCarrinho(Carrinho carrinho, Prato prato, Integer quantidade) {
        CarrinhoItem item = new CarrinhoItem();
        item.setCarrinho(carrinho);
        item.setPrato(prato);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(prato.getPreco());
        item.setSubtotal(prato.getPreco().multiply(BigDecimal.valueOf(quantidade))
                .setScale(SCALE, ROUNDING_MODE));
        return item;
    }

    private CarrinhoItem buscarItemNoCarrinho(Carrinho carrinho, Long itemId) {
        return carrinho.getItens().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado no carrinho"));
    }

    private void recalcularValores(Carrinho carrinho) {
        BigDecimal subtotal = carrinho.getItens().stream()
                .map(CarrinhoItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING_MODE);
        
        carrinho.setSubtotal(subtotal);
        
        BigDecimal desconto = aplicarDescontoCupom(carrinho, subtotal);
        carrinho.setDesconto(desconto);
        carrinho.setTotal(subtotal.subtract(desconto).setScale(SCALE, ROUNDING_MODE));
    }

    private BigDecimal aplicarDescontoCupom(Carrinho carrinho, BigDecimal subtotal) {
        if (carrinho.getCupom() == null) {
            return BigDecimal.ZERO;
        }
        
        Cupom cupom = carrinho.getCupom();
        BigDecimal desconto;
        
        if (cupom.getTipoDesconto().name().equals("PERCENTUAL")) {
            desconto = CalculadoraFinanceira.calcularTaxaPlataforma(subtotal, cupom.getValorDesconto());
        } else {
            desconto = cupom.getValorDesconto();
            if (desconto.compareTo(subtotal) > 0) {
                desconto = subtotal;
            }
        }
        
        return desconto.setScale(SCALE, ROUNDING_MODE);
    }
}

