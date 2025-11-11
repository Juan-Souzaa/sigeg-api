package com.siseg.service;

import com.siseg.dto.pedido.PedidoItemRequestDTO;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.*;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.TipoDesconto;
import com.siseg.repository.*;
import com.siseg.mapper.PedidoMapper;
import com.siseg.util.SecurityUtils;
import com.siseg.util.TempoEstimadoCalculator;
import com.siseg.util.CalculadoraFinanceira;
import com.siseg.util.VehicleConstants;
import com.siseg.validator.PedidoValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

@Service
public class PedidoService {
    
    private static final Logger logger = Logger.getLogger(PedidoService.class.getName());
    private static final BigDecimal VALOR_MINIMO_TAXA_ENTREGA = new BigDecimal("50.00");
    private static final BigDecimal TAXA_ENTREGA_PADRAO = new BigDecimal("5.00");
    
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final RestauranteRepository restauranteRepository;
    private final PratoRepository pratoRepository;
    private final EntregadorRepository entregadorRepository;
    private final NotificationService notificationService;
    private final GeocodingService geocodingService;
    private final RastreamentoService rastreamentoService;
    private final TempoEstimadoCalculator tempoEstimadoCalculator;
    private final PedidoMapper pedidoMapper;
    private final PedidoValidator pedidoValidator;
    private final CarrinhoService carrinhoService;
    private final CupomService cupomService;
    private final TaxaCalculoService taxaCalculoService;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
                         RestauranteRepository restauranteRepository, PratoRepository pratoRepository,
                         EntregadorRepository entregadorRepository, NotificationService notificationService,
                         GeocodingService geocodingService, RastreamentoService rastreamentoService,
                         TempoEstimadoCalculator tempoEstimadoCalculator, PedidoMapper pedidoMapper,
                         PedidoValidator pedidoValidator, CarrinhoService carrinhoService, CupomService cupomService,
                         TaxaCalculoService taxaCalculoService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.pratoRepository = pratoRepository;
        this.entregadorRepository = entregadorRepository;
        this.notificationService = notificationService;
        this.geocodingService = geocodingService;
        this.rastreamentoService = rastreamentoService;
        this.tempoEstimadoCalculator = tempoEstimadoCalculator;
        this.pedidoMapper = pedidoMapper;
        this.pedidoValidator = pedidoValidator;
        this.carrinhoService = carrinhoService;
        this.cupomService = cupomService;
        this.taxaCalculoService = taxaCalculoService;
    }
    
    @Transactional
    public PedidoResponseDTO criarPedido(Long clienteId, PedidoRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = obterOuValidarCliente(clienteId, currentUser);
        Restaurante restaurante = buscarRestaurante(dto.getRestauranteId());
        
        Pedido pedido = criarPedidoBasico(cliente, restaurante, dto);
        processarGeocodificacaoEndereco(pedido, cliente, dto.getEnderecoEntrega());
        
        if (dto.getCarrinhoId() != null) {
            processarCarrinhoParaPedido(pedido, dto.getCarrinhoId(), cliente.getId());
        } else {
            processarItensPedido(pedido, dto.getItens());
        }
        
        calcularValoresPedido(pedido);
        
        Pedido saved = pedidoRepository.save(pedido);
        
        if (dto.getCarrinhoId() != null) {
            carrinhoService.limparCarrinho();
        }
        
        return pedidoMapper.toResponseDTO(saved);
    }
    
    private Cliente obterOuValidarCliente(Long clienteId, User currentUser) {
        if (clienteId == null) {
            return buscarClientePorUsuario(currentUser);
        }
        
        Cliente cliente = buscarClientePorId(clienteId);
        pedidoValidator.validatePermissaoCliente(cliente, currentUser);
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
    
    private void processarItensPedido(Pedido pedido, List<PedidoItemRequestDTO> itensDto) {
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
        
        return pedidoValidator.validatePratoDisponivel(prato);
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
    
    private void calcularValoresPedido(Pedido pedido) {
        pedido.setTaxaEntrega(calcularTaxaEntrega(pedido.getSubtotal()));
        pedido.setTotal(pedido.getSubtotal().add(pedido.getTaxaEntrega()));
    }
    
    private void processarCarrinhoParaPedido(Pedido pedido, Long carrinhoId, Long clienteId) {
        Carrinho carrinho = validarECarregarCarrinho(carrinhoId, clienteId);
        BigDecimal subtotal = processarItensDoCarrinho(pedido, carrinho);
        aplicarCupomSeExistir(pedido, carrinho, subtotal);
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
        if (carrinho.getCupom() != null) {
            BigDecimal desconto = calcularDescontoCupom(carrinho.getCupom(), subtotal);
            BigDecimal taxaEntrega = calcularTaxaEntrega(subtotal);
            pedido.setTotal(subtotal.subtract(desconto).add(taxaEntrega));
            cupomService.incrementarUsoCupom(carrinho.getCupom());
        } else {
            pedido.setTotal(subtotal.add(calcularTaxaEntrega(subtotal)));
        }
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
    
    private BigDecimal calcularDescontoCupom(Cupom cupom, BigDecimal subtotal) {
        if (cupom.getTipoDesconto() == TipoDesconto.PERCENTUAL) {
            return CalculadoraFinanceira.calcularTaxaPlataforma(subtotal, cupom.getValorDesconto());
        }
        BigDecimal desconto = cupom.getValorDesconto();
        return desconto.compareTo(subtotal) > 0 ? subtotal : desconto;
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
        Pedido pedido = buscarPedidoValido(id);
        validatePedidoOwnership(pedido);
        pedidoValidator.validateStatusParaConfirmacao(pedido);
        
        pedido.setStatus(StatusPedido.CONFIRMED);
        Pedido saved = pedidoRepository.save(pedido);
        
        enviarNotificacoesConfirmacaoPedido(saved);
        
        return pedidoMapper.toResponseDTO(saved);
    }
    
    private void enviarNotificacoesConfirmacaoPedido(Pedido pedido) {
        notificarClienteStatusPedido(pedido, "CONFIRMED");
        notificarRestauranteNovoPedido(pedido);
    }
    
    private void notificarRestauranteNovoPedido(Pedido pedido) {
        if (pedido.getRestaurante() != null) {
            notificationService.notifyRestaurantNewOrder(
                pedido.getId(),
                pedido.getRestaurante().getEmail(),
                pedido.getTotal()
            );
        }
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
        Pedido pedido = buscarPedidoValido(id);
        pedidoValidator.validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode atualizar este status");
        pedidoValidator.validateStatusParaSaiuEntrega(pedido);
        
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        inicializarPosicaoEntregadorSeNecessario(pedido);
        
        Pedido saved = pedidoRepository.save(pedido);
        notificarClienteStatusPedido(saved, "OUT_FOR_DELIVERY");
        
        return pedidoMapper.toResponseDTO(saved);
    }
    
    private void inicializarPosicaoEntregadorSeNecessario(Pedido pedido) {
        if (pedido.getEntregador() == null) {
            return;
        }
        
        Entregador entregador = pedido.getEntregador();
        if (precisaInicializarPosicao(entregador, pedido.getRestaurante())) {
            definirPosicaoInicialEntregador(entregador, pedido.getRestaurante());
        }
    }
    
    private boolean precisaInicializarPosicao(Entregador entregador, Restaurante restaurante) {
        return (entregador.getLatitude() == null || entregador.getLongitude() == null) &&
               restaurante != null &&
               restaurante.getLatitude() != null &&
               restaurante.getLongitude() != null;
    }
    
    private void definirPosicaoInicialEntregador(Entregador entregador, Restaurante restaurante) {
        entregador.setLatitude(restaurante.getLatitude());
        entregador.setLongitude(restaurante.getLongitude());
        entregadorRepository.save(entregador);
        logger.info("Posição inicial do entregador definida com coordenadas do restaurante");
    }

    @Transactional
    public PedidoResponseDTO marcarComoEntregue(Long id) {
        Pedido pedido = buscarPedidoValido(id);
        pedidoValidator.validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode marcar como entregue");
        pedidoValidator.validateStatusParaEntrega(pedido);
        
        pedido.setStatus(StatusPedido.DELIVERED);
        taxaCalculoService.calcularEAtualizarValoresFinanceiros(pedido);
        Pedido saved = pedidoRepository.save(pedido);
        
        logger.info("Pedido " + saved.getId() + " marcado como entregue. Cliente pode criar avaliação agora.");
        
        enviarNotificacoesEntregaPedido(saved);
        
        return pedidoMapper.toResponseDTO(saved);
    }
    
    private void enviarNotificacoesEntregaPedido(Pedido pedido) {
        notificarClienteStatusPedido(pedido, "DELIVERED");
        notificarRestauranteNovoPedido(pedido);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPedidosDisponiveis(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não é entregador"));
        
        if (entregador.getStatus() != StatusEntregador.APPROVED) {
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
            Duration tempoEstimado = Duration.ofMinutes(resultado.getTempoMinutos());
            pedido.setTempoEstimadoEntrega(Instant.now().plus(tempoEstimado));
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
        Duration tempoEstimado = Duration.ofMinutes(VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS);
        pedido.setTempoEstimadoEntrega(Instant.now().plus(tempoEstimado));
        logger.warning("Coordenadas não disponíveis, usando tempo padrão de " 
            + VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS + " minutos");
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
    
    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarMeusPedidos(StatusPedido status, Instant dataInicio, Instant dataFim, Long restauranteId, Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        
        Page<Pedido> pedidos = buscarPedidosComFiltros(cliente.getId(), status, dataInicio, dataFim, restauranteId, pageable);
        
        return pedidos.map(pedidoMapper::toResponseDTO);
    }
    
    private Page<Pedido> buscarPedidosComFiltros(Long clienteId, StatusPedido status, Instant dataInicio, Instant dataFim, Long restauranteId, Pageable pageable) {
        boolean temStatus = status != null;
        boolean temPeriodo = dataInicio != null && dataFim != null;
        boolean temRestaurante = restauranteId != null;
        
        if (temStatus && temPeriodo) {
            return pedidoRepository.findByClienteIdAndStatusAndCriadoEmBetween(
                clienteId, status, dataInicio, dataFim, pageable
            );
        }
        
        if (temStatus) {
            return pedidoRepository.findByClienteIdAndStatus(clienteId, status, pageable);
        }
        
        if (temPeriodo) {
            return pedidoRepository.findByClienteIdAndCriadoEmBetween(
                clienteId, dataInicio, dataFim, pageable
            );
        }
        
        if (temRestaurante) {
            return pedidoRepository.findByClienteIdAndRestauranteId(clienteId, restauranteId, pageable);
        }
        
        return pedidoRepository.findByClienteId(clienteId, pageable);
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        SecurityUtils.validatePedidoOwnership(pedido);
    }
    
    private BigDecimal calcularTaxaEntrega(BigDecimal subtotal) {
        if (subtotal.compareTo(VALOR_MINIMO_TAXA_ENTREGA) >= 0) {
            return BigDecimal.ZERO;
        }
        return TAXA_ENTREGA_PADRAO;
    }
    
}
