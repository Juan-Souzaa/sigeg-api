package com.siseg.service;

import com.siseg.dto.avaliacao.AvaliacaoRequestDTO;
import com.siseg.dto.avaliacao.AvaliacaoResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoResumoDTO;
import com.siseg.dto.avaliacao.AvaliacaoRestauranteResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoEntregadorResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoResumoEntregadorDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.AvaliacaoAlreadyExistsException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Avaliacao;
import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.util.SecurityUtils;
import org.modelmapper.ModelMapper;
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
    private final ModelMapper modelMapper;
    
    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository, 
                           PedidoRepository pedidoRepository,
                           ClienteRepository clienteRepository,
                           ModelMapper modelMapper) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.modelMapper = modelMapper;
    }
    
    @Transactional
    public AvaliacaoResponseDTO criarAvaliacao(Long pedidoId, AvaliacaoRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        Cliente cliente = clienteRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário autenticado"));
        
        if (!SecurityUtils.isAdmin()) {
            if (pedido.getCliente() == null || pedido.getCliente().getUser() == null ||
                !pedido.getCliente().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Você só pode avaliar seus próprios pedidos");
            }
        }
        
        if (pedido.getStatus() != StatusPedido.DELIVERED) {
            throw new IllegalStateException("Apenas pedidos entregues (DELIVERED) podem ser avaliados");
        }
        
        if (avaliacaoRepository.existsByClienteIdAndPedidoId(cliente.getId(), pedidoId)) {
            throw new AvaliacaoAlreadyExistsException("Já existe uma avaliação para este pedido. Use o endpoint de edição para atualizar.");
        }
        
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setPedido(pedido);
        avaliacao.setCliente(cliente);
        avaliacao.setRestaurante(pedido.getRestaurante());
        avaliacao.setEntregador(pedido.getEntregador());
        avaliacao.setNotaRestaurante(dto.getNotaRestaurante());
        avaliacao.setNotaPedido(dto.getNotaPedido());
        avaliacao.setComentarioRestaurante(dto.getComentarioRestaurante());
        avaliacao.setComentarioPedido(dto.getComentarioPedido());
        
        if (dto.getNotaEntregador() != null) {
            if (pedido.getEntregador() == null) {
                throw new IllegalStateException("Não é possível avaliar entregador: pedido não possui entregador associado");
            }
            avaliacao.setNotaEntregador(dto.getNotaEntregador());
            avaliacao.setComentarioEntregador(dto.getComentarioEntregador());
        }
        
        Avaliacao saved = avaliacaoRepository.save(avaliacao);
        logger.info("Avaliação criada para pedido " + pedidoId + " pelo cliente " + cliente.getId());
        
        return mapearParaResponse(saved);
    }
    
    @Transactional
    public AvaliacaoResponseDTO editarAvaliacao(Long avaliacaoId, AvaliacaoRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada com ID: " + avaliacaoId));
        
        if (!SecurityUtils.isAdmin()) {
            if (avaliacao.getCliente() == null || avaliacao.getCliente().getUser() == null ||
                !avaliacao.getCliente().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Você só pode editar suas próprias avaliações");
            }
        }
        
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
        
        avaliacao.setAtualizadoEm(Instant.now());
        Avaliacao saved = avaliacaoRepository.save(avaliacao);
        logger.info("Avaliação " + avaliacaoId + " editada pelo cliente " + avaliacao.getCliente().getId());
        
        return mapearParaResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public AvaliacaoResponseDTO buscarAvaliacaoPorPedido(Long pedidoId) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com ID: " + pedidoId));
        
        if (!SecurityUtils.isAdmin()) {
            if (pedido.getCliente() == null || pedido.getCliente().getUser() == null ||
                !pedido.getCliente().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Você só pode consultar avaliações de seus próprios pedidos");
            }
        }
        
        Avaliacao avaliacao = avaliacaoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada para o pedido " + pedidoId));
        
        return mapearParaResponse(avaliacao);
    }
    
    @Transactional(readOnly = true)
    public Page<AvaliacaoRestauranteResponseDTO> listarAvaliacoesPorRestaurante(Long restauranteId, Pageable pageable) {
        Page<Avaliacao> avaliacoes = avaliacaoRepository.findByRestauranteIdOrderByCriadoEmDesc(restauranteId, pageable);
        return avaliacoes.map(this::mapearParaResponseRestaurante);
    }
    
    @Transactional(readOnly = true)
    public Page<AvaliacaoEntregadorResponseDTO> listarAvaliacoesPorEntregador(Long entregadorId, Pageable pageable) {
        Page<Avaliacao> avaliacoes = avaliacaoRepository.findByEntregadorIdOrderByCriadoEmDesc(entregadorId, pageable);
        return avaliacoes.map(this::mapearParaResponseEntregador);
    }
    
    @Transactional(readOnly = true)
    public AvaliacaoResumoEntregadorDTO obterResumoEntregador(Long entregadorId) {
        AvaliacaoResumoEntregadorDTO resumo = new AvaliacaoResumoEntregadorDTO();
        resumo.setMediaNotaEntregador(calcularMediaEntregador(entregadorId));
        resumo.setTotalAvaliacoesEntregador(contarAvaliacoesEntregador(entregadorId));
        return resumo;
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
        AvaliacaoResumoDTO resumo = new AvaliacaoResumoDTO();
        resumo.setMediaNotaRestaurante(calcularMediaRestaurante(restauranteId));
        resumo.setTotalAvaliacoesRestaurante(contarAvaliacoesRestaurante(restauranteId));
        return resumo;
    }
    
    private AvaliacaoResponseDTO mapearParaResponse(Avaliacao avaliacao) {
        AvaliacaoResponseDTO dto = modelMapper.map(avaliacao, AvaliacaoResponseDTO.class);
        dto.setPedidoId(avaliacao.getPedido().getId());
        dto.setClienteId(avaliacao.getCliente().getId());
        dto.setRestauranteId(avaliacao.getRestaurante().getId());
        if (avaliacao.getEntregador() != null) {
            dto.setEntregadorId(avaliacao.getEntregador().getId());
        }
        return dto;
    }
    
    /**
     * Mapeia avaliação para response específico de restaurante (apenas dados do restaurante e pedido)
     */
    private AvaliacaoRestauranteResponseDTO mapearParaResponseRestaurante(Avaliacao avaliacao) {
        AvaliacaoRestauranteResponseDTO dto = new AvaliacaoRestauranteResponseDTO();
        dto.setId(avaliacao.getId());
        dto.setPedidoId(avaliacao.getPedido().getId());
        dto.setClienteId(avaliacao.getCliente().getId());
        dto.setRestauranteId(avaliacao.getRestaurante().getId());
        dto.setNotaRestaurante(avaliacao.getNotaRestaurante());
        dto.setNotaPedido(avaliacao.getNotaPedido());
        dto.setComentarioRestaurante(avaliacao.getComentarioRestaurante());
        dto.setComentarioPedido(avaliacao.getComentarioPedido());
        dto.setCriadoEm(avaliacao.getCriadoEm());
        dto.setAtualizadoEm(avaliacao.getAtualizadoEm());
        return dto;
    }
    
    /**
     * Mapeia avaliação para response específico de entregador (apenas dados do entregador)
     */
    private AvaliacaoEntregadorResponseDTO mapearParaResponseEntregador(Avaliacao avaliacao) {
        AvaliacaoEntregadorResponseDTO dto = new AvaliacaoEntregadorResponseDTO();
        dto.setId(avaliacao.getId());
        dto.setPedidoId(avaliacao.getPedido().getId());
        dto.setClienteId(avaliacao.getCliente().getId());
        if (avaliacao.getEntregador() != null) {
            dto.setEntregadorId(avaliacao.getEntregador().getId());
        }
        dto.setNotaEntregador(avaliacao.getNotaEntregador());
        dto.setComentarioEntregador(avaliacao.getComentarioEntregador());
        dto.setCriadoEm(avaliacao.getCriadoEm());
        dto.setAtualizadoEm(avaliacao.getAtualizadoEm());
        return dto;
    }
}

