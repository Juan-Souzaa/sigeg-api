package com.siseg.service;

import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.*;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.mapper.PedidoMapper;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.PedidoValidator;
import com.siseg.service.pedido.PedidoEnderecoService;
import com.siseg.service.pedido.PedidoFinanceiroService;
import com.siseg.service.pedido.PedidoNotificacaoService;
import com.siseg.service.pedido.PedidoEntregadorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.logging.Logger;

@Service
public class PedidoService {
    
    private static final Logger logger = Logger.getLogger(PedidoService.class.getName());
    
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final RestauranteRepository restauranteRepository;
    private final RastreamentoService rastreamentoService;
    private final PedidoMapper pedidoMapper;
    private final PedidoValidator pedidoValidator;
    private final PedidoEnderecoService pedidoEnderecoService;
    private final PedidoFinanceiroService pedidoFinanceiroService;
    private final PedidoNotificacaoService pedidoNotificacaoService;
    private final PedidoEntregadorService pedidoEntregadorService;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
                         RestauranteRepository restauranteRepository,
                         RastreamentoService rastreamentoService, PedidoMapper pedidoMapper,
                         PedidoValidator pedidoValidator, PedidoEnderecoService pedidoEnderecoService,
                         PedidoFinanceiroService pedidoFinanceiroService,
                         PedidoNotificacaoService pedidoNotificacaoService,
                         PedidoEntregadorService pedidoEntregadorService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.rastreamentoService = rastreamentoService;
        this.pedidoMapper = pedidoMapper;
        this.pedidoValidator = pedidoValidator;
        this.pedidoEnderecoService = pedidoEnderecoService;
        this.pedidoFinanceiroService = pedidoFinanceiroService;
        this.pedidoNotificacaoService = pedidoNotificacaoService;
        this.pedidoEntregadorService = pedidoEntregadorService;
    }
    
    @Transactional
    public PedidoResponseDTO criarPedido(Long clienteId, PedidoRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = obterOuValidarCliente(clienteId, currentUser);
        Restaurante restaurante = buscarRestaurante(dto.getRestauranteId());
        
        Pedido pedido = criarPedidoBasico(cliente, restaurante, dto);
        pedidoEnderecoService.processarEnderecoEntrega(pedido, cliente, dto);
        
        if (dto.getCarrinhoId() != null) {
            pedidoFinanceiroService.processarCarrinhoParaPedido(pedido, dto.getCarrinhoId(), cliente.getId());
        } else {
            pedidoFinanceiroService.processarItensPedido(pedido, dto.getItens());
            pedidoFinanceiroService.calcularValoresPedido(pedido);
        }
        
        Pedido saved = pedidoRepository.save(pedido);
        
        if (dto.getCarrinhoId() != null) {
            pedidoFinanceiroService.limparCarrinho();
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
        pedido.setStatus(StatusPedido.CREATED);
        return pedido;
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
        
        pedidoNotificacaoService.enviarNotificacoesConfirmacaoPedido(saved);
        
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
        
        pedidoNotificacaoService.notificarClienteStatusPedido(saved, "PREPARING");
        
        return pedidoMapper.toResponseDTO(saved);
    }

    @Transactional
    public PedidoResponseDTO marcarSaiuEntrega(Long id) {
        return pedidoEntregadorService.marcarSaiuEntrega(id);
    }
    

    @Transactional
    public PedidoResponseDTO marcarComoEntregue(Long id) {
        return pedidoEntregadorService.marcarComoEntregue(id);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPedidosDisponiveis(Pageable pageable) {
        return pedidoEntregadorService.listarPedidosDisponiveis(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarEntregasAtivas(Pageable pageable) {
        return pedidoEntregadorService.listarEntregasAtivas(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarHistoricoEntregas(Pageable pageable) {
        return pedidoEntregadorService.listarHistoricoEntregas(pageable);
    }

    @Transactional
    public PedidoResponseDTO aceitarPedido(Long pedidoId) {
        return pedidoEntregadorService.aceitarPedido(pedidoId);
    }
    
    
    
    public void recusarPedido(Long pedidoId) {
        pedidoEntregadorService.recusarPedido(pedidoId);
    }
    
    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarMeusPedidos(StatusPedido status, Instant dataInicio, Instant dataFim, Long restauranteId, Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = buscarClientePorUsuario(currentUser);
        
        Page<Pedido> pedidos = buscarPedidosComFiltros(cliente.getId(), status, dataInicio, dataFim, restauranteId, pageable);
        
        return pedidos.map(pedidoMapper::toResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPedidosRestaurante(StatusPedido status, Instant dataInicio, Instant dataFim, Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        Restaurante restaurante = pedidoValidator.validateRestauranteAprovado(currentUser);
        
        Page<Pedido> pedidos = buscarPedidosRestauranteComFiltros(restaurante.getId(), status, dataInicio, dataFim, pageable);
        
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
    
    @Transactional
    public PedidoResponseDTO cancelarPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + id));
        
        SecurityUtils.validatePedidoOwnership(pedido);
        
        if (pedido.getStatus() != StatusPedido.CREATED && pedido.getStatus() != StatusPedido.CONFIRMED) {
            throw new IllegalStateException("Só é possível cancelar pedidos com status CREATED ou CONFIRMED");
        }
        
        if (pedido.getStatus() == StatusPedido.OUT_FOR_DELIVERY || pedido.getStatus() == StatusPedido.DELIVERED) {
            throw new IllegalStateException("Não é possível cancelar pedido que já saiu para entrega ou foi entregue");
        }
        
        pedidoFinanceiroService.processarReembolsoSeNecessario(pedido);
        
        pedido.setStatus(StatusPedido.CANCELED);
        Pedido saved = pedidoRepository.save(pedido);
        
        logger.info("Pedido " + id + " cancelado");
        
        return pedidoMapper.toResponseDTO(saved);
    }
    
    private Page<Pedido> buscarPedidosRestauranteComFiltros(Long restauranteId, StatusPedido status, Instant dataInicio, Instant dataFim, Pageable pageable) {
        boolean temStatus = status != null;
        boolean temPeriodo = dataInicio != null && dataFim != null;
        
        if (temStatus && temPeriodo) {
            return pedidoRepository.findByRestauranteIdAndStatusAndCriadoEmBetween(
                restauranteId, status, dataInicio, dataFim, pageable
            );
        }
        
        if (temStatus) {
            return pedidoRepository.findByRestauranteIdAndStatus(restauranteId, status, pageable);
        }
        
        if (temPeriodo) {
            return pedidoRepository.findByRestauranteIdAndCriadoEmBetween(
                restauranteId, dataInicio, dataFim, pageable
            );
        }
        
        return pedidoRepository.findByRestauranteId(restauranteId, pageable);
    }
    
    private Pedido buscarPedidoValido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
    }
    
    private void validatePedidoOwnership(Pedido pedido) {
        SecurityUtils.validatePedidoOwnership(pedido);
    }
    
    @Transactional
    public void atualizarStatusPorPagamentoConfirmado(Long pedidoId) {
        Pedido pedido = buscarPedidoValido(pedidoId);

        logger.info("notificacao por webhook recebida para o pedido " + pedidoId);
        
        if (pedido.getStatus() == StatusPedido.CREATED) {
            pedido.setStatus(StatusPedido.CONFIRMED);
            pedidoRepository.save(pedido);
            
            pedidoNotificacaoService.enviarNotificacoesConfirmacaoPedido(pedido);
            logger.info("Pedido " + pedidoId + " confirmado automaticamente após pagamento confirmado");
        }
    }
}
