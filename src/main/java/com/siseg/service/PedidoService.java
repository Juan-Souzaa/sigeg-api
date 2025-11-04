package com.siseg.service;

import com.siseg.dto.cardapio.CardapioResponseDTO;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.dto.restaurante.RestauranteBuscaDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.exception.PedidoAlreadyProcessedException;
import com.siseg.exception.PratoNotAvailableException;
import com.siseg.model.*;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.*;
import com.siseg.mapper.PedidoMapper;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.PedidoValidator;
import com.siseg.util.TempoEstimadoCalculator;
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
    private final RastreamentoService rastreamentoService;
    private final AvaliacaoService avaliacaoService;
    private final ModelMapper modelMapper;
    private final TempoEstimadoCalculator tempoEstimadoCalculator;
    private final PedidoMapper pedidoMapper;
    private final PedidoValidator pedidoValidator;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
                         RestauranteRepository restauranteRepository, PratoRepository pratoRepository,
                         EntregadorRepository entregadorRepository, NotificationService notificationService,
                         GeocodingService geocodingService, RastreamentoService rastreamentoService,
                         AvaliacaoService avaliacaoService, ModelMapper modelMapper,
                         TempoEstimadoCalculator tempoEstimadoCalculator, PedidoMapper pedidoMapper,
                         PedidoValidator pedidoValidator) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.pratoRepository = pratoRepository;
        this.entregadorRepository = entregadorRepository;
        this.notificationService = notificationService;
        this.geocodingService = geocodingService;
        this.rastreamentoService = rastreamentoService;
        this.avaliacaoService = avaliacaoService;
        this.modelMapper = modelMapper;
        this.tempoEstimadoCalculator = tempoEstimadoCalculator;
        this.pedidoMapper = pedidoMapper;
        this.pedidoValidator = pedidoValidator;
    }
    
    @Transactional
    public PedidoResponseDTO criarPedido(Long clienteId, PedidoRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = obterOuValidarCliente(clienteId, currentUser);
        Restaurante restaurante = buscarRestaurante(dto.getRestauranteId());
        
        Pedido pedido = criarPedidoBasico(cliente, restaurante, dto);
        processarGeocodificacaoEndereco(pedido, cliente, dto.getEnderecoEntrega());
        processarItensPedido(pedido, dto.getItens());
        calcularValoresPedido(pedido);
        
        Pedido saved = pedidoRepository.save(pedido);
        return pedidoMapper.toResponseDTO(saved);
    }
    
    private Cliente obterOuValidarCliente(Long clienteId, User currentUser) {
        if (clienteId == null) {
            return buscarClientePorUsuario(currentUser);
        }
        
        Cliente cliente = buscarClientePorId(clienteId);
        validarPermissaoCliente(cliente, currentUser);
        return cliente;
    }
    
    private Cliente buscarClientePorUsuario(User currentUser) {
        return clienteRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário autenticado"));
    }
    
    private Cliente buscarClientePorId(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));
    }
    
    private void validarPermissaoCliente(Cliente cliente, User currentUser) {
        if (!SecurityUtils.isAdmin() && (cliente.getUser() == null || !cliente.getUser().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException("Você não tem permissão para criar pedidos para este cliente");
        }
    }
    
    private Restaurante buscarRestaurante(Long restauranteId) {
        return restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
    }
    
    private Pedido criarPedidoBasico(Cliente cliente, Restaurante restaurante, PedidoRequestDTO dto) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setMetodoPagamento(dto.getMetodoPagamento());
        pedido.setTroco(dto.getTroco());
        pedido.setObservacoes(dto.getObservacoes());
        pedido.setEnderecoEntrega(dto.getEnderecoEntrega());
        pedido.setStatus(StatusPedido.CREATED);
        return pedido;
    }
    
    private void processarGeocodificacaoEndereco(Pedido pedido, Cliente cliente, String enderecoEntrega) {
        if (podeReutilizarCoordenadasCliente(cliente, enderecoEntrega)) {
            pedido.setLatitudeEntrega(cliente.getLatitude());
            pedido.setLongitudeEntrega(cliente.getLongitude());
            logger.info("Coordenadas reutilizadas do cliente para endereço de entrega");
            return;
        }
        
        geocodingService.geocodeAddress(enderecoEntrega)
                .ifPresentOrElse(
                    coords -> {
                        pedido.setLatitudeEntrega(coords.getLatitude());
                        pedido.setLongitudeEntrega(coords.getLongitude());
                        logger.info("Coordenadas geocodificadas para endereço de entrega: " + enderecoEntrega);
                    },
                    () -> logger.warning("Não foi possível geocodificar endereço de entrega: " + enderecoEntrega)
                );
    }
    
    private boolean podeReutilizarCoordenadasCliente(Cliente cliente, String enderecoEntrega) {
        return cliente.getEndereco() != null && 
               cliente.getEndereco().equals(enderecoEntrega) &&
               cliente.getLatitude() != null && 
               cliente.getLongitude() != null;
    }
    
    private void processarItensPedido(Pedido pedido, java.util.List<com.siseg.dto.pedido.PedidoItemRequestDTO> itensDto) {
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (var itemDto : itensDto) {
            Prato prato = buscarPratoDisponivel(itemDto.getPratoId());
            PedidoItem item = criarPedidoItem(pedido, prato, itemDto);
            pedido.getItens().add(item);
            subtotal = subtotal.add(item.getSubtotal());
        }
        
        pedido.setSubtotal(subtotal);
    }
    
    private Prato buscarPratoDisponivel(Long pratoId) {
        Prato prato = pratoRepository.findById(pratoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com ID: " + pratoId));
        
        if (!prato.getDisponivel()) {
            throw new PratoNotAvailableException("Prato não disponível: " + prato.getNome());
        }
        
        return prato;
    }
    
    private PedidoItem criarPedidoItem(Pedido pedido, Prato prato, com.siseg.dto.pedido.PedidoItemRequestDTO itemDto) {
        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setPrato(prato);
        item.setQuantidade(itemDto.getQuantidade());
        item.setPrecoUnitario(prato.getPreco());
        item.setSubtotal(prato.getPreco().multiply(BigDecimal.valueOf(itemDto.getQuantidade())));
        return item;
    }
    
    private void calcularValoresPedido(Pedido pedido) {
        pedido.setTaxaEntrega(calcularTaxaEntrega(pedido.getSubtotal()));
        pedido.setTotal(pedido.getSubtotal().add(pedido.getTaxaEntrega()));
    }
    
    public PedidoResponseDTO buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        validatePedidoOwnership(pedido);
        
        PedidoResponseDTO response = pedidoMapper.toResponseDTO(pedido);
        
        if (pedido.getStatus() == StatusPedido.OUT_FOR_DELIVERY) {
            try {
                response.setRastreamento(rastreamentoService.obterRastreamento(id));
            } catch (Exception e) {
                logger.warning("Erro ao obter rastreamento para pedido " + id + ": " + e.getMessage());
            }
        }
        
        return response;
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
        
        return pedidoMapper.toResponseDTO(saved);
    }

    @Transactional
    public PedidoResponseDTO marcarComoPreparando(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        SecurityUtils.validateRestauranteOwnership(pedido.getRestaurante());
        pedidoValidator.validateStatusPreparo(pedido);
        
        pedido.setStatus(StatusPedido.PREPARING);
        Pedido saved = pedidoRepository.save(pedido);
        
        notificarClienteStatusPedido(saved, "PREPARING");
        
        return pedidoMapper.toResponseDTO(saved);
    }
    private void notificarClienteStatusPedido(Pedido pedido, String status) {
        if (pedido.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                pedido.getId(),
                pedido.getCliente().getEmail(),
                pedido.getCliente().getTelefone(),
                status
            );
        }
    }

    @Transactional
    public PedidoResponseDTO marcarSaiuEntrega(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        User currentUser = SecurityUtils.getCurrentUser();
        
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
        
        if (pedido.getEntregador() != null) {
            Entregador entregador = pedido.getEntregador();
            if ((entregador.getLatitude() == null || entregador.getLongitude() == null) &&
                pedido.getRestaurante() != null &&
                pedido.getRestaurante().getLatitude() != null &&
                pedido.getRestaurante().getLongitude() != null) {
                entregador.setLatitude(pedido.getRestaurante().getLatitude());
                entregador.setLongitude(pedido.getRestaurante().getLongitude());
                entregadorRepository.save(entregador);
                logger.info("Posição inicial do entregador definida com coordenadas do restaurante");
            }
        }
        
        Pedido saved = pedidoRepository.save(pedido);
        
        if (saved.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                saved.getId(),
                saved.getCliente().getEmail(),
                saved.getCliente().getTelefone(),
                "OUT_FOR_DELIVERY"
            );
        }
        
        return pedidoMapper.toResponseDTO(saved);
    }

    @Transactional
    public PedidoResponseDTO marcarComoEntregue(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        User currentUser = SecurityUtils.getCurrentUser();
        
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
        
        logger.info("Pedido " + saved.getId() + " marcado como entregue. Cliente pode criar avaliação agora.");
        
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
        
        return pedidoMapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPedidosDisponiveis(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não é entregador"));
        
        if (entregador.getStatus() != com.siseg.model.enumerations.StatusEntregador.APPROVED) {
            throw new AccessDeniedException("Entregador não está aprovado");
        }
        
        Page<Pedido> pedidos = pedidoRepository.findByStatusAndEntregadorIsNull(StatusPedido.PREPARING, pageable);
        
        pedidos.getContent().forEach(pedido -> {
            notificationService.notifyNewOrderAvailable(
                pedido.getId(),
                entregador.getEmail(),
                entregador.getTelefone(),
                pedido.getEnderecoEntrega(),
                pedido.getTotal()
            );
        });
        
        return pedidos.map(pedidoMapper::toResponseDTO);
    }

    @Transactional
    public PedidoResponseDTO aceitarPedido(Long pedidoId) {
        User currentUser = SecurityUtils.getCurrentUser();
        Entregador entregador = pedidoValidator.validateEntregadorAprovado(currentUser);
        Pedido pedido = buscarPedidoValido(pedidoId);
        
        pedidoValidator.validatePedidoAceitavel(pedido);
        
        pedido.setEntregador(entregador);
        calcularEAtualizarTempoEstimadoEntrega(pedido, entregador);
        
        Pedido saved = pedidoRepository.save(pedido);
        enviarNotificacoesAceitePedido(saved);
        
        return pedidoMapper.toResponseDTO(saved);
    }
    
    private Pedido buscarPedidoValido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
    }
    
    private void calcularEAtualizarTempoEstimadoEntrega(Pedido pedido, Entregador entregador) {
        if (!temCoordenadasCompletas(pedido)) {
            aplicarTempoPadraoEntrega(pedido);
            return;
        }
        
        var resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
            pedido.getRestaurante().getLatitude(),
            pedido.getRestaurante().getLongitude(),
            pedido.getLatitudeEntrega(),
            pedido.getLongitudeEntrega(),
            entregador.getTipoVeiculo()
        );
        
        if (resultado.getDistanciaKm() != null && resultado.getTempoMinutos() > 0) {
            java.time.Duration tempoEstimado = java.time.Duration.ofMinutes(resultado.getTempoMinutos());
            pedido.setTempoEstimadoEntrega(java.time.Instant.now().plus(tempoEstimado));
            logger.info(String.format("Tempo estimado calculado: %d minutos para distância de %s km (OSRM: %s)",
                resultado.getTempoMinutos(), resultado.getDistanciaKm(), resultado.isUsadoOSRM()));
        } else {
            aplicarTempoPadraoEntrega(pedido);
        }
    }
    
    private boolean temCoordenadasCompletas(Pedido pedido) {
        return pedido.getRestaurante() != null && 
               pedido.getRestaurante().getLatitude() != null && 
               pedido.getRestaurante().getLongitude() != null &&
               pedido.getLatitudeEntrega() != null && 
               pedido.getLongitudeEntrega() != null;
    }
    
    private void aplicarTempoPadraoEntrega(Pedido pedido) {
        java.time.Duration tempoEstimado = java.time.Duration.ofMinutes(
            com.siseg.util.VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS);
        pedido.setTempoEstimadoEntrega(java.time.Instant.now().plus(tempoEstimado));
        logger.warning("Coordenadas não disponíveis, usando tempo padrão de " 
            + com.siseg.util.VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS + " minutos");
    }
    
    private void enviarNotificacoesAceitePedido(Pedido pedido) {
        if (pedido.getRestaurante() != null) {
            notificationService.notifyRestaurantNewOrder(
                pedido.getId(),
                pedido.getRestaurante().getEmail(),
                pedido.getTotal()
            );
        }
        
        if (pedido.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                pedido.getId(),
                pedido.getCliente().getEmail(),
                pedido.getCliente().getTelefone(),
                "ACEITO_POR_ENTREGADOR"
            );
        }
    }
    
    public void recusarPedido(Long pedidoId) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não é entregador"));
        
        pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        logger.info(String.format("Entregador %s recusou o pedido %d", entregador.getNome(), pedidoId));
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        SecurityUtils.validatePedidoOwnership(pedido);
    }
    
    @Transactional(readOnly = true)
    public Page<RestauranteBuscaDTO> buscarRestaurantes(String cozinha, Pageable pageable) {
        Page<Restaurante> restaurantes = restauranteRepository.buscarRestaurantesAprovados(cozinha, pageable);
        
        var coordenadasCliente = obterCoordenadasCliente();
        
        return restaurantes.map(restaurante -> {
            RestauranteBuscaDTO dto = modelMapper.map(restaurante, RestauranteBuscaDTO.class);
            
            if (coordenadasCliente != null && temCoordenadas(restaurante)) {
                calcularDistanciaETempoRestaurante(dto, restaurante, coordenadasCliente);
            }
            
            adicionarAvaliacoesRestaurante(dto, restaurante.getId());
            
            return dto;
        });
    }
    
    private Coordenadas obterCoordenadasCliente() {
        try {
            User currentUser = SecurityUtils.getCurrentUser();
            if (currentUser == null) {
                return null;
            }
            
            return buscarCoordenadasCliente(currentUser);
        } catch (Exception e) {
            logger.warning("Erro ao obter coordenadas do cliente: " + e.getMessage());
            return null;
        }
    }
    
    private Coordenadas buscarCoordenadasCliente(User currentUser) {
        Cliente cliente = clienteRepository.findByUserId(currentUser.getId()).orElse(null);
        if (cliente == null) {
            return null;
        }
        
        if (temCoordenadasValidas(cliente)) {
            return new Coordenadas(cliente.getLatitude(), cliente.getLongitude());
        }
        
        return null;
    }
    
    private boolean temCoordenadasValidas(Cliente cliente) {
        return cliente.getLatitude() != null && cliente.getLongitude() != null;
    }
    
    private boolean temCoordenadas(Restaurante restaurante) {
        return restaurante.getLatitude() != null && restaurante.getLongitude() != null;
    }
    
    private void calcularDistanciaETempoRestaurante(RestauranteBuscaDTO dto, Restaurante restaurante, Coordenadas coordenadasCliente) {
        var resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
            coordenadasCliente.latitude(),
            coordenadasCliente.longitude(),
            restaurante.getLatitude(),
            restaurante.getLongitude(),
            null
        );
        
        if (resultado.getDistanciaKm() != null) {
            dto.setDistanciaKm(resultado.getDistanciaKm());
            dto.setTempoEstimadoMinutos(resultado.getTempoMinutos());
        }
    }
    
    private void adicionarAvaliacoesRestaurante(RestauranteBuscaDTO dto, Long restauranteId) {
        BigDecimal mediaAvaliacao = avaliacaoService.calcularMediaRestaurante(restauranteId);
        long totalAvaliacoes = avaliacaoService.contarAvaliacoesRestaurante(restauranteId);
        
        dto.setMediaAvaliacao(mediaAvaliacao);
        dto.setTotalAvaliacoes(totalAvaliacoes);
    }
    
    private record Coordenadas(BigDecimal latitude, BigDecimal longitude) {}
    
    public Page<CardapioResponseDTO> buscarCardapio(Long restauranteId, Pageable pageable) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
        
        Page<Prato> pratosPage = pratoRepository.findByRestauranteIdAndDisponivel(restauranteId, true, pageable);
        
        Map<CategoriaMenu, List<Prato>> pratosPorCategoria = pratosPage.getContent().stream()
                .collect(Collectors.groupingBy(Prato::getCategoria));
        
        CardapioResponseDTO response = pedidoMapper.toCardapioResponseDTO(
            restauranteId, restaurante.getNome(), pratosPorCategoria);
        
        return new PageImpl<>(List.of(response), pageable, pratosPage.getTotalElements());
    }
    
    private BigDecimal calcularTaxaEntrega(BigDecimal subtotal) {
        if (subtotal.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(5.0);
    }
    
}
