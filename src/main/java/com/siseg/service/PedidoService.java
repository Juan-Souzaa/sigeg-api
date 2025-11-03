package com.siseg.service;

import com.siseg.dto.cardapio.CardapioResponseDTO;
import com.siseg.dto.cardapio.CategoriaCardapioDTO;
import com.siseg.dto.cardapio.PratoCardapioDTO;
import com.siseg.dto.entregador.EntregadorSimplesDTO;
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
import com.siseg.util.DistanceCalculator;
import com.siseg.util.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class PedidoService {
    
    private static final Logger logger = Logger.getLogger(PedidoService.class.getName());
    
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final RestauranteRepository restauranteRepository;
    private final PratoRepository pratoRepository;
    private final EntregadorRepository entregadorRepository;
    private final NotificationService notificationService;
    private final GeocodingService geocodingService;
    private final ModelMapper modelMapper;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
                         RestauranteRepository restauranteRepository, PratoRepository pratoRepository,
                         EntregadorRepository entregadorRepository, NotificationService notificationService,
                         GeocodingService geocodingService, ModelMapper modelMapper) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.pratoRepository = pratoRepository;
        this.entregadorRepository = entregadorRepository;
        this.notificationService = notificationService;
        this.geocodingService = geocodingService;
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
        
        // Geocodificar endereço de entrega automaticamente
        // Otimização: se o endereço for o mesmo do cliente e ele já tiver coordenadas, reutilizar
        if (cliente.getEndereco() != null && cliente.getEndereco().equals(dto.getEnderecoEntrega()) 
            && cliente.getLatitude() != null && cliente.getLongitude() != null) {
            pedido.setLatitudeEntrega(cliente.getLatitude());
            pedido.setLongitudeEntrega(cliente.getLongitude());
            logger.info("Coordenadas reutilizadas do cliente para endereço de entrega");
        } else {
            geocodingService.geocodeAddress(dto.getEnderecoEntrega())
                    .ifPresentOrElse(
                        coords -> {
                            pedido.setLatitudeEntrega(coords.getLatitude());
                            pedido.setLongitudeEntrega(coords.getLongitude());
                            logger.info("Coordenadas geocodificadas para endereço de entrega: " + dto.getEnderecoEntrega());
                        },
                        () -> logger.warning("Não foi possível geocodificar endereço de entrega: " + dto.getEnderecoEntrega())
                    );
        }
        
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
        
        // Notificar cliente e restaurante
        if (saved.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                saved.getId(), 
                saved.getCliente().getEmail(),
                saved.getCliente().getTelefone(),
                "CONFIRMED"
            );
        }
        if (saved.getRestaurante() != null) {
            notificationService.notifyRestaurantNewOrder(
                saved.getId(),
                saved.getRestaurante().getEmail(),
                saved.getTotal()
            );
        }
        
        return mapearParaResponse(saved);
    }

    @Transactional
    public PedidoResponseDTO marcarComoPreparando(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Apenas o dono do restaurante pode marcar como preparando
        if (!SecurityUtils.isAdmin() && (pedido.getRestaurante() == null || pedido.getRestaurante().getUser() == null ||
            !pedido.getRestaurante().getUser().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Você não tem permissão para atualizar este pedido");
        }
        
        if (pedido.getStatus() != StatusPedido.CONFIRMED) {
            throw new PedidoAlreadyProcessedException("Pedido deve estar CONFIRMED para ser marcado como PREPARING");
        }
        
        pedido.setStatus(StatusPedido.PREPARING);
        Pedido saved = pedidoRepository.save(pedido);
        
        // Notificar cliente
        if (saved.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                saved.getId(),
                saved.getCliente().getEmail(),
                saved.getCliente().getTelefone(),
                "PREPARING"
            );
        }
        
        return mapearParaResponse(saved);
    }

    @Transactional
    public PedidoResponseDTO marcarSaiuEntrega(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Apenas o entregador associado ao pedido pode marcar como saiu para entrega
        if (!SecurityUtils.isAdmin()) {
            if (pedido.getEntregador() == null || pedido.getEntregador().getUser() == null ||
                !pedido.getEntregador().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Apenas o entregador associado pode atualizar este status");
            }
        }
        
        if (pedido.getStatus() != StatusPedido.PREPARING) {
            throw new PedidoAlreadyProcessedException("Pedido deve estar PREPARING para ser marcado como OUT_FOR_DELIVERY");
        }
        
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        Pedido saved = pedidoRepository.save(pedido);
        
        // Notificar cliente
        if (saved.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                saved.getId(),
                saved.getCliente().getEmail(),
                saved.getCliente().getTelefone(),
                "OUT_FOR_DELIVERY"
            );
        }
        
        return mapearParaResponse(saved);
    }

    @Transactional
    public PedidoResponseDTO marcarComoEntregue(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Apenas o entregador associado ao pedido pode marcar como entregue
        if (!SecurityUtils.isAdmin()) {
            if (pedido.getEntregador() == null || pedido.getEntregador().getUser() == null ||
                !pedido.getEntregador().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Apenas o entregador associado pode marcar como entregue");
            }
        }
        
        if (pedido.getStatus() != StatusPedido.OUT_FOR_DELIVERY) {
            throw new PedidoAlreadyProcessedException("Pedido deve estar OUT_FOR_DELIVERY para ser marcado como DELIVERED");
        }
        
        pedido.setStatus(StatusPedido.DELIVERED);
        Pedido saved = pedidoRepository.save(pedido);
        
        // Notificar cliente e restaurante
        if (saved.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                saved.getId(),
                saved.getCliente().getEmail(),
                saved.getCliente().getTelefone(),
                "DELIVERED"
            );
        }
        if (saved.getRestaurante() != null) {
            notificationService.notifyRestaurantNewOrder(
                saved.getId(),
                saved.getRestaurante().getEmail(),
                saved.getTotal()
            );
        }
        
        return mapearParaResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPedidosDisponiveis(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Verificar se usuário é entregador aprovado
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não é entregador"));
        
        if (entregador.getStatus() != com.siseg.model.enumerations.StatusEntregador.APPROVED) {
            throw new AccessDeniedException("Entregador não está aprovado");
        }
        
        Page<Pedido> pedidos = pedidoRepository.findByStatusAndEntregadorIsNull(StatusPedido.PREPARING, pageable);
        
        // Notificar entregadores sobre novos pedidos disponíveis (opcional - pode ser otimizado)
        pedidos.getContent().forEach(pedido -> {
            notificationService.notifyNewOrderAvailable(
                pedido.getId(),
                entregador.getEmail(),
                entregador.getTelefone(),
                pedido.getEnderecoEntrega(),
                pedido.getTotal()
            );
        });
        
        return pedidos.map(this::mapearParaResponse);
    }

    @Transactional
    public PedidoResponseDTO aceitarPedido(Long pedidoId) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Verificar se usuário é entregador aprovado
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não é entregador"));
        
        if (entregador.getStatus() != com.siseg.model.enumerations.StatusEntregador.APPROVED) {
            throw new AccessDeniedException("Entregador não está aprovado");
        }
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        if (pedido.getStatus() != StatusPedido.PREPARING) {
            throw new PedidoAlreadyProcessedException("Pedido deve estar PREPARING para ser aceito");
        }
        
        if (pedido.getEntregador() != null) {
            throw new PedidoAlreadyProcessedException("Pedido já foi aceito por outro entregador");
        }
        
        pedido.setEntregador(entregador);
        
        // Calcular tempo estimado de entrega baseado na distância real
        // Distância = do restaurante até o endereço de entrega
        if (pedido.getRestaurante() != null && 
            pedido.getRestaurante().getLatitude() != null && 
            pedido.getRestaurante().getLongitude() != null &&
            pedido.getLatitudeEntrega() != null && 
            pedido.getLongitudeEntrega() != null) {
            
            // Calcula distância do restaurante até o endereço de entrega
            BigDecimal distanciaKm = DistanceCalculator.calculateDistance(
                pedido.getRestaurante().getLatitude(),
                pedido.getRestaurante().getLongitude(),
                pedido.getLatitudeEntrega(),
                pedido.getLongitudeEntrega()
            );
            
            if (distanciaKm != null && distanciaKm.compareTo(BigDecimal.ZERO) > 0) {
                int tempoMinutos = DistanceCalculator.estimateDeliveryTime(
                    distanciaKm, 
                    entregador.getTipoVeiculo().name()
                );
                java.time.Duration tempoEstimado = java.time.Duration.ofMinutes(tempoMinutos);
                pedido.setTempoEstimadoEntrega(java.time.Instant.now().plus(tempoEstimado));
                logger.info("Tempo estimado calculado: " + tempoMinutos + " minutos para distância de " + distanciaKm + " km");
            } else {
                // Fallback para 30 minutos se não conseguir calcular ou distância inválida
                java.time.Duration tempoEstimado = java.time.Duration.ofMinutes(30);
                pedido.setTempoEstimadoEntrega(java.time.Instant.now().plus(tempoEstimado));
                logger.warning("Distância inválida ou zero, usando tempo padrão de 30 minutos");
            }
        } else {
            // Fallback para 30 minutos se não houver coordenadas do restaurante ou do endereço de entrega
            java.time.Duration tempoEstimado = java.time.Duration.ofMinutes(30);
            pedido.setTempoEstimadoEntrega(java.time.Instant.now().plus(tempoEstimado));
            logger.warning("Coordenadas não disponíveis para restaurante ou endereço de entrega, usando tempo padrão de 30 minutos");
        }
        
        Pedido saved = pedidoRepository.save(pedido);
        
        // Notificar restaurante e cliente
        if (saved.getRestaurante() != null) {
            notificationService.notifyRestaurantNewOrder(
                saved.getId(),
                saved.getRestaurante().getEmail(),
                saved.getTotal()
            );
        }
        if (saved.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                saved.getId(),
                saved.getCliente().getEmail(),
                saved.getCliente().getTelefone(),
                "ACEITO_POR_ENTREGADOR"
            );
        }
        
        return mapearParaResponse(saved);
    }
    
    /**
     * Recusar pedido (apenas log, não persiste)
     * Método opcional mencionado no plano
     */
    public void recusarPedido(Long pedidoId) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não é entregador"));
        
        // Verificar se pedido existe
        pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        logger.info(String.format("Entregador %s recusou o pedido %d", entregador.getNome(), pedidoId));
        // Não persiste nada, apenas log para estatísticas futuras
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Admin pode acessar qualquer pedido
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        // Verifica se o pedido pertence ao cliente autenticado
        if (pedido.getCliente() != null && pedido.getCliente().getUser() != null && 
            pedido.getCliente().getUser().getId().equals(currentUser.getId())) {
            return;
        }
        
        // Verifica se é dono do restaurante
        if (pedido.getRestaurante() != null && pedido.getRestaurante().getUser() != null &&
            pedido.getRestaurante().getUser().getId().equals(currentUser.getId())) {
            return;
        }
        
        // Verifica se é o entregador associado ao pedido
        if (pedido.getEntregador() != null && pedido.getEntregador().getUser() != null &&
            pedido.getEntregador().getUser().getId().equals(currentUser.getId())) {
            return;
        }
        
        throw new AccessDeniedException("Você não tem permissão para acessar este pedido");
    }
    
    @Transactional(readOnly = true)
    public Page<RestauranteBuscaDTO> buscarRestaurantes(String cozinha, Pageable pageable) {
        Page<Restaurante> restaurantes = restauranteRepository.buscarRestaurantesAprovados(cozinha, pageable);
        
        BigDecimal clienteLat = null;
        BigDecimal clienteLon = null;
        
        try {
            User currentUser = SecurityUtils.getCurrentUser();
            if (currentUser != null) {
                Cliente cliente = clienteRepository.findByUserId(currentUser.getId()).orElse(null);
                if (cliente != null && cliente.getLatitude() != null && cliente.getLongitude() != null) {
                    clienteLat = cliente.getLatitude();
                    clienteLon = cliente.getLongitude();
                }
            }
        } catch (Exception e) {
            logger.fine("Não foi possível obter coordenadas do cliente: " + e.getMessage());
        }
        
        final BigDecimal finalClienteLat = clienteLat;
        final BigDecimal finalClienteLon = clienteLon;
        
        return restaurantes.map(r -> {
            RestauranteBuscaDTO dto = modelMapper.map(r, RestauranteBuscaDTO.class);
            
            if (finalClienteLat != null && finalClienteLon != null && 
                r.getLatitude() != null && r.getLongitude() != null) {
                
                BigDecimal distanciaKm = DistanceCalculator.calculateDistance(
                    finalClienteLat,
                    finalClienteLon,
                    r.getLatitude(),
                    r.getLongitude()
                );
                
                if (distanciaKm != null && distanciaKm.compareTo(BigDecimal.ZERO) > 0) {
                    dto.setDistanciaKm(distanciaKm);
                    int tempoMinutos = DistanceCalculator.estimateDeliveryTime(distanciaKm, "MOTO");
                    dto.setTempoEstimadoMinutos(tempoMinutos);
                } else {
                    dto.setDistanciaKm(null);
                    dto.setTempoEstimadoMinutos(null);
                }
            } else {
                dto.setDistanciaKm(null);
                dto.setTempoEstimadoMinutos(null);
            }
            
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
        
        // Incluir informações do entregador se houver
        if (pedido.getEntregador() != null) {
            EntregadorSimplesDTO entregadorDto = new EntregadorSimplesDTO();
            entregadorDto.setId(pedido.getEntregador().getId());
            entregadorDto.setNome(pedido.getEntregador().getNome());
            entregadorDto.setTelefone(pedido.getEntregador().getTelefone());
            response.setEntregador(entregadorDto);
        }
        
        response.setTempoEstimadoEntrega(pedido.getTempoEstimadoEntrega());
        
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
