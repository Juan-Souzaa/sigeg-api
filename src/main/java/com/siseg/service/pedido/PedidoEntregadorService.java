package com.siseg.service.pedido;

import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.mapper.PedidoMapper;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.PedidoValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Logger;

@Service
public class PedidoEntregadorService {

    private static final Logger logger = Logger.getLogger(PedidoEntregadorService.class.getName());

    private final PedidoRepository pedidoRepository;
    private final EntregadorRepository entregadorRepository;
    private final PedidoMapper pedidoMapper;
    private final PedidoValidator pedidoValidator;
    private final PedidoEnderecoService pedidoEnderecoService;
    private final PedidoFinanceiroService pedidoFinanceiroService;
    private final PedidoNotificacaoService pedidoNotificacaoService;

    public PedidoEntregadorService(PedidoRepository pedidoRepository,
                                   EntregadorRepository entregadorRepository,
                                   PedidoMapper pedidoMapper,
                                   PedidoValidator pedidoValidator,
                                   PedidoEnderecoService pedidoEnderecoService,
                                   PedidoFinanceiroService pedidoFinanceiroService,
                                   PedidoNotificacaoService pedidoNotificacaoService) {
        this.pedidoRepository = pedidoRepository;
        this.entregadorRepository = entregadorRepository;
        this.pedidoMapper = pedidoMapper;
        this.pedidoValidator = pedidoValidator;
        this.pedidoEnderecoService = pedidoEnderecoService;
        this.pedidoFinanceiroService = pedidoFinanceiroService;
        this.pedidoNotificacaoService = pedidoNotificacaoService;
    }

    @Transactional
    public PedidoResponseDTO marcarSaiuEntrega(Long id) {
        Pedido pedido = buscarPedidoValido(id);
        pedidoValidator.validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode atualizar este status");
        pedidoValidator.validateStatusParaSaiuEntrega(pedido);

        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);
        pedidoEnderecoService.inicializarPosicaoEntregadorSeNecessario(pedido);

        Pedido saved = pedidoRepository.save(pedido);
        pedidoNotificacaoService.notificarClienteStatusPedido(saved, "OUT_FOR_DELIVERY");

        return pedidoMapper.toResponseDTO(saved);
    }

    @Transactional
    public PedidoResponseDTO marcarComoEntregue(Long id) {
        Pedido pedido = buscarPedidoValido(id);
        pedidoValidator.validateEntregadorDoPedido(pedido, "Apenas o entregador associado pode marcar como entregue");
        pedidoValidator.validateStatusParaEntrega(pedido);

        pedido.setStatus(StatusPedido.DELIVERED);
        pedidoFinanceiroService.calcularEAtualizarValoresPosEntrega(pedido);
        Pedido saved = pedidoRepository.save(pedido);

        logger.info("Pedido " + saved.getId() + " marcado como entregue. Cliente pode criar avaliação agora.");
        pedidoNotificacaoService.enviarNotificacoesEntregaPedido(saved);

        return pedidoMapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPedidosDisponiveis(Pageable pageable) {
        Entregador entregador = obterEntregadorAutenticado();

        if (entregador.getStatus() != StatusEntregador.APPROVED) {
            throw new AccessDeniedException("Entregador não está aprovado");
        }

        Page<Pedido> pedidos = pedidoRepository.findByStatusAndEntregadorIsNull(StatusPedido.PREPARING, pageable);

        pedidos.getContent().forEach(pedido ->
            pedidoNotificacaoService.notificarPedidoDisponivelParaEntregador(pedido, entregador)
        );

        return pedidos.map(pedidoMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarEntregasAtivas(Pageable pageable) {
        Entregador entregador = obterEntregadorAutenticado();

        Page<Pedido> pedidos = pedidoRepository.findByEntregadorIdAndStatusNotIn(
                entregador.getId(),
                List.of(StatusPedido.DELIVERED, StatusPedido.CANCELED),
                pageable
        );

        return pedidos.map(pedidoMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarHistoricoEntregas(Pageable pageable) {
        Entregador entregador = obterEntregadorAutenticado();

        Page<Pedido> pedidos = pedidoRepository.findByEntregadorIdAndStatus(
                entregador.getId(),
                StatusPedido.DELIVERED,
                pageable
        );

        return pedidos.map(pedidoMapper::toResponseDTO);
    }

    @Transactional
    public PedidoResponseDTO aceitarPedido(Long pedidoId) {
        Entregador entregador = pedidoValidator.validateEntregadorAprovado(SecurityUtils.getCurrentUser());
        Pedido pedido = buscarPedidoValido(pedidoId);

        pedidoValidator.validatePedidoAceitavel(pedido);

        pedido.setEntregador(entregador);
        pedidoEnderecoService.calcularEAtualizarTempoEstimadoEntrega(pedido, entregador);

        Pedido saved = pedidoRepository.save(pedido);
        pedidoNotificacaoService.enviarNotificacoesAceitePedido(saved);

        return pedidoMapper.toResponseDTO(saved);
    }

    public void recusarPedido(Long pedidoId) {
        Entregador entregador = obterEntregadorAutenticado();

        pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));

        logger.info(String.format("Entregador %s recusou o pedido %d", entregador.getNome(), pedidoId));
    }

    private Entregador obterEntregadorAutenticado() {
        var currentUser = SecurityUtils.getCurrentUser();
        return entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não é entregador"));
    }

    private Pedido buscarPedidoValido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
    }
}

