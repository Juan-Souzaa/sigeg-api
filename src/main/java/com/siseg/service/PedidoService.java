package com.siseg.service;

import com.siseg.dto.cardapio.CardapioResponseDTO;
import com.siseg.dto.cardapio.CategoriaCardapioDTO;
import com.siseg.dto.cardapio.PratoCardapioDTO;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.dto.pedido.PedidoItemResponseDTO;
import com.siseg.dto.restaurante.RestauranteBuscaDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.exception.PedidoAlreadyProcessedException;
import com.siseg.exception.PratoNotAvailableException;
import com.siseg.model.*;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.*;
import com.siseg.util.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PedidoService {
    
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final RestauranteRepository restauranteRepository;
    private final PratoRepository pratoRepository;
    private final ModelMapper modelMapper;
    
    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
                        RestauranteRepository restauranteRepository, PratoRepository pratoRepository,
                        ModelMapper modelMapper) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.pratoRepository = pratoRepository;
        this.modelMapper = modelMapper;
    }
    
    @Transactional
    public PedidoResponseDTO criarPedido(Long clienteId, PedidoRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Cliente cliente;
        
        // Se clienteId for fornecido, valida ownership. Senão, obtém automaticamente do usuário autenticado
        if (clienteId != null) {
            cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));
            
            // Valida se o clienteId pertence ao usuário autenticado
            if (!SecurityUtils.isAdmin() && (cliente.getUser() == null || !cliente.getUser().getId().equals(currentUser.getId()))) {
                throw new AccessDeniedException("Você não tem permissão para criar pedidos para este cliente");
            }
        } else {
            // Obtém o cliente do usuário autenticado
            cliente = clienteRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário autenticado"));
        }
        
        Restaurante restaurante = restauranteRepository.findById(dto.getRestauranteId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + dto.getRestauranteId()));
        
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setMetodoPagamento(dto.getMetodoPagamento());
        pedido.setTroco(dto.getTroco());
        pedido.setObservacoes(dto.getObservacoes());
        pedido.setEnderecoEntrega(dto.getEnderecoEntrega());
        pedido.setStatus(StatusPedido.CREATED);
        
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (var itemDto : dto.getItens()) {
            Prato prato = pratoRepository.findById(itemDto.getPratoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com ID: " + itemDto.getPratoId()));
            
            if (!prato.getDisponivel()) {
                throw new PratoNotAvailableException("Prato não disponível: " + prato.getNome());
            }
            
            PedidoItem item = new PedidoItem();
            item.setPedido(pedido);
            item.setPrato(prato);
            item.setQuantidade(itemDto.getQuantidade());
            item.setPrecoUnitario(prato.getPreco());
            item.setSubtotal(prato.getPreco().multiply(BigDecimal.valueOf(itemDto.getQuantidade())));
            
            pedido.getItens().add(item);
            subtotal = subtotal.add(item.getSubtotal());
        }
        
        pedido.setSubtotal(subtotal);
        pedido.setTaxaEntrega(calcularTaxaEntrega(subtotal));
        pedido.setTotal(pedido.getSubtotal().add(pedido.getTaxaEntrega()));
        
        Pedido saved = pedidoRepository.save(pedido);
        return mapearParaResponse(saved);
    }
    
    public PedidoResponseDTO buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        validatePedidoOwnership(pedido);
        
        return mapearParaResponse(pedido);
    }
    
    @Transactional
    public PedidoResponseDTO confirmarPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        validatePedidoOwnership(pedido);
        
        if (pedido.getStatus() != StatusPedido.CREATED) {
            throw new PedidoAlreadyProcessedException("Pedido já foi processado");
        }
        
        pedido.setStatus(StatusPedido.CONFIRMED);
        Pedido saved = pedidoRepository.save(pedido);
        
        return mapearParaResponse(saved);
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Admin pode acessar qualquer pedido
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        // Verifica se o pedido pertence ao cliente autenticado
        if (pedido.getCliente() == null || pedido.getCliente().getUser() == null || 
            !pedido.getCliente().getUser().getId().equals(currentUser.getId())) {
            
            // Também verifica se é dono do restaurante
            if (pedido.getRestaurante() == null || pedido.getRestaurante().getUser() == null ||
                !pedido.getRestaurante().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Você não tem permissão para acessar este pedido");
            }
        }
    }
    
    public Page<RestauranteBuscaDTO> buscarRestaurantes(String cozinha, Pageable pageable) {
        Page<Restaurante> restaurantes = restauranteRepository.buscarRestaurantesAprovados(cozinha, pageable);
        
        return restaurantes.map(r -> {
            RestauranteBuscaDTO dto = modelMapper.map(r, RestauranteBuscaDTO.class);
            dto.setDistanciaKm(BigDecimal.valueOf(Math.random() * 10).setScale(2, RoundingMode.HALF_UP));
            dto.setTempoEstimadoMinutos((int) (Math.random() * 30) + 15);
            return dto;
        });
    }
    
    public Page<CardapioResponseDTO> buscarCardapio(Long restauranteId, Pageable pageable) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
        
        // Buscar pratos com paginação
        Page<Prato> pratosPage = pratoRepository.findByRestauranteIdAndDisponivel(restauranteId, true, pageable);
        
        // Agrupar por categoria
        Map<CategoriaMenu, List<Prato>> pratosPorCategoria = pratosPage.getContent().stream()
                .collect(Collectors.groupingBy(Prato::getCategoria));
        
        CardapioResponseDTO response = new CardapioResponseDTO();
        response.setRestauranteId(restauranteId);
        response.setRestauranteNome(restaurante.getNome());
        
        List<CategoriaCardapioDTO> categorias = pratosPorCategoria.entrySet().stream()
                .map(entry -> {
                    CategoriaCardapioDTO categoria = new CategoriaCardapioDTO();
                    categoria.setCategoria(entry.getKey());
                    categoria.setPratos(entry.getValue().stream()
                            .map(p -> modelMapper.map(p, PratoCardapioDTO.class))
                            .toList());
                    return categoria;
                })
                .toList();
        
        response.setCategorias(categorias);
        
        // Retornar como Page com os metadados corretos
        return new PageImpl<>(List.of(response), pageable, pratosPage.getTotalElements());
    }
    
    private BigDecimal calcularTaxaEntrega(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(5.0);
    }
    
    private PedidoResponseDTO mapearParaResponse(Pedido pedido) {
        PedidoResponseDTO response = modelMapper.map(pedido, PedidoResponseDTO.class);
        response.setClienteId(pedido.getCliente().getId());
        response.setRestauranteId(pedido.getRestaurante().getId());
        
        List<PedidoItemResponseDTO> itens = pedido.getItens().stream()
                .map(item -> {
                    PedidoItemResponseDTO itemDto = modelMapper.map(item, PedidoItemResponseDTO.class);
                    itemDto.setPratoId(item.getPrato().getId());
                    itemDto.setPratoNome(item.getPrato().getNome());
                    return itemDto;
                })
                .toList();
        
        response.setItens(itens);
        return response;
    }
}
