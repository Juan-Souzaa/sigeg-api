package com.siseg.service;

import com.siseg.dto.avaliacao.AvaliacaoRequestDTO;
import com.siseg.dto.avaliacao.AvaliacaoResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoResumoDTO;
import com.siseg.dto.avaliacao.AvaliacaoRestauranteResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoEntregadorResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoResumoEntregadorDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Avaliacao;
import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.mapper.AvaliacaoMapper;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.AvaliacaoValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.logging.Logger;

@Service
public class AvaliacaoService {
    
    private static final Logger logger = Logger.getLogger(AvaliacaoService.class.getName());
    
    private final AvaliacaoRepository avaliacaoRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final AvaliacaoMapper avaliacaoMapper;
    private final AvaliacaoValidator avaliacaoValidator;
    
    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, 
                           PedidoRepository pedidoRepository,
                           ClienteRepository clienteRepository,
                           AvaliacaoMapper avaliacaoMapper,
                           AvaliacaoValidator avaliacaoValidator) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.avaliacaoMapper = avaliacaoMapper;
        this.avaliacaoValidator = avaliacaoValidator;
    }
    
    @Transactional
    public AvaliacaoResponseDTO criarAvaliacao(Long pedidoId, AvaliacaoRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        Pedido pedido = buscarPedidoValido(pedidoId);
        Cliente cliente = buscarCliente(currentUser);
        
        avaliacaoValidator.validatePermissaoAvaliacao(pedido);
        avaliacaoValidator.validatePedidoEntregue(pedido);
        avaliacaoValidator.validateAvaliacaoNaoExistente(cliente.getId(), pedidoId);
        
        Avaliacao avaliacao = criarAvaliacaoBasica(pedido, cliente, dto);
        processarAvaliacaoEntregador(avaliacao, pedido, dto);
        
        Avaliacao saved = avaliacaoRepository.save(avaliacao);
        logger.info("Avaliação criada para pedido " + pedidoId + " pelo cliente " + cliente.getId());
        
        return avaliacaoMapper.toResponseDTO(saved);
    }
    
    private Pedido buscarPedidoValido(Long pedidoId) {
        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
    }
    
    private Cliente buscarCliente(User currentUser) {
        return clienteRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário autenticado"));
    }
    
    
    private Avaliacao criarAvaliacaoBasica(Pedido pedido, Cliente cliente, AvaliacaoRequestDTO dto) {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setPedido(pedido);
        avaliacao.setCliente(cliente);
        avaliacao.setRestaurante(pedido.getRestaurante());
        avaliacao.setEntregador(pedido.getEntregador());
        avaliacao.setNotaRestaurante(dto.getNotaRestaurante());
        avaliacao.setNotaPedido(dto.getNotaPedido());
        avaliacao.setComentarioRestaurante(dto.getComentarioRestaurante());
        avaliacao.setComentarioPedido(dto.getComentarioPedido());
        return avaliacao;
    }
    
    private void processarAvaliacaoEntregador(Avaliacao avaliacao, Pedido pedido, AvaliacaoRequestDTO dto) {
        if (dto.getNotaEntregador() != null) {
            if (pedido.getEntregador() == null) {
                throw new IllegalStateException("Não é possível avaliar entregador: pedido não possui entregador associado");
            }
            avaliacao.setNotaEntregador(dto.getNotaEntregador());
            avaliacao.setComentarioEntregador(dto.getComentarioEntregador());
        }
    }
    
    @Transactional
    public AvaliacaoResponseDTO editarAvaliacao(Long avaliacaoId, AvaliacaoRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        Avaliacao avaliacao = buscarAvaliacao(avaliacaoId);
        
        if (!SecurityUtils.isAdmin()) {
            avaliacaoValidator.validateOwnership(avaliacao, currentUser);
        }
        
        atualizarDadosAvaliacao(avaliacao, dto);
        avaliacao.setAtualizadoEm(Instant.now());
        
        Avaliacao saved = avaliacaoRepository.save(avaliacao);
        logger.info("Avaliação " + avaliacaoId + " editada pelo cliente " + avaliacao.getCliente().getId());
        
        return avaliacaoMapper.toResponseDTO(saved);
    }
    
    private Avaliacao buscarAvaliacao(Long avaliacaoId) {
        return avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada com ID: " + avaliacaoId));
    }
    
    
    private void atualizarDadosAvaliacao(Avaliacao avaliacao, AvaliacaoRequestDTO dto) {
        avaliacao.setNotaRestaurante(dto.getNotaRestaurante());
        avaliacao.setNotaPedido(dto.getNotaPedido());
        avaliacao.setComentarioRestaurante(dto.getComentarioRestaurante());
        avaliacao.setComentarioPedido(dto.getComentarioPedido());
        
        if (dto.getNotaEntregador() != null) {
            if (avaliacao.getPedido().getEntregador() == null) {
                throw new IllegalStateException("Não é possível avaliar entregador: pedido não possui entregador associado");
            }
            avaliacao.setNotaEntregador(dto.getNotaEntregador());
            avaliacao.setComentarioEntregador(dto.getComentarioEntregador());
        } else {
            avaliacao.setNotaEntregador(null);
            avaliacao.setComentarioEntregador(null);
        }
    }
    
    @Transactional(readOnly = true)
    public AvaliacaoResponseDTO buscarAvaliacaoPorPedido(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.validatePedidoOwnership(pedido);
        }
        
        Avaliacao avaliacao = avaliacaoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada para o pedido " + pedidoId));
        
        return avaliacaoMapper.toResponseDTO(avaliacao);
    }
    
    @Transactional(readOnly = true)
    public Page<AvaliacaoRestauranteResponseDTO> listarAvaliacoesPorRestaurante(Long restauranteId, Pageable pageable) {
        Page<Avaliacao> avaliacoes = avaliacaoRepository.findByRestauranteIdOrderByCriadoEmDesc(restauranteId, pageable);
        return avaliacoes.map(avaliacaoMapper::toRestauranteResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<AvaliacaoEntregadorResponseDTO> listarAvaliacoesPorEntregador(Long entregadorId, Pageable pageable) {
        Page<Avaliacao> avaliacoes = avaliacaoRepository.findByEntregadorIdOrderByCriadoEmDesc(entregadorId, pageable);
        return avaliacoes.map(avaliacaoMapper::toEntregadorResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public AvaliacaoResumoEntregadorDTO obterResumoEntregador(Long entregadorId) {
        return avaliacaoMapper.toResumoEntregadorDTO(
            calcularMediaEntregador(entregadorId),
            contarAvaliacoesEntregador(entregadorId)
        );
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calcularMediaRestaurante(Long restauranteId) {
        BigDecimal media = avaliacaoRepository.calcularMediaNotaRestaurante(restauranteId);
        if (media == null) {
            return null;
        }
        return media.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Transactional(readOnly = true)
    public BigDecimal calcularMediaEntregador(Long entregadorId) {
        BigDecimal media = avaliacaoRepository.calcularMediaNotaEntregador(entregadorId);
        if (media == null) {
            return null;
        }
        return media.setScale(2, RoundingMode.HALF_UP);
    }
    
    @Transactional(readOnly = true)
    public long contarAvaliacoesRestaurante(Long restauranteId) {
        return avaliacaoRepository.countByRestauranteId(restauranteId);
    }
    
    @Transactional(readOnly = true)
    public long contarAvaliacoesEntregador(Long entregadorId) {
        return avaliacaoRepository.countByEntregadorId(entregadorId);
    }
    
    @Transactional(readOnly = true)
    public AvaliacaoResumoDTO obterResumoRestaurante(Long restauranteId) {
        return avaliacaoMapper.toResumoDTO(
            calcularMediaRestaurante(restauranteId),
            contarAvaliacoesRestaurante(restauranteId)
        );
    }
}

