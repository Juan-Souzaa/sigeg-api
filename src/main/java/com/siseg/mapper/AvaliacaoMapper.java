package com.siseg.mapper;

import com.siseg.dto.avaliacao.AvaliacaoEntregadorResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoResponseDTO;
import com.siseg.dto.avaliacao.AvaliacaoResumoDTO;
import com.siseg.dto.avaliacao.AvaliacaoResumoEntregadorDTO;
import com.siseg.dto.avaliacao.AvaliacaoRestauranteResponseDTO;
import com.siseg.model.Avaliacao;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AvaliacaoMapper {
    
    private final ModelMapper modelMapper;
    
    public AvaliacaoMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }
    
    public AvaliacaoResponseDTO toResponseDTO(Avaliacao avaliacao) {
        AvaliacaoResponseDTO dto = modelMapper.map(avaliacao, AvaliacaoResponseDTO.class);
        dto.setPedidoId(avaliacao.getPedido().getId());
        dto.setClienteId(avaliacao.getCliente().getId());
        dto.setRestauranteId(avaliacao.getRestaurante().getId());
        if (avaliacao.getEntregador() != null) {
            dto.setEntregadorId(avaliacao.getEntregador().getId());
        }
        return dto;
    }
    
    public AvaliacaoRestauranteResponseDTO toRestauranteResponseDTO(Avaliacao avaliacao) {
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
    
    public AvaliacaoEntregadorResponseDTO toEntregadorResponseDTO(Avaliacao avaliacao) {
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
    
    public AvaliacaoResumoDTO toResumoDTO(BigDecimal mediaNotaRestaurante, long totalAvaliacoesRestaurante) {
        AvaliacaoResumoDTO resumo = new AvaliacaoResumoDTO();
        resumo.setMediaNotaRestaurante(mediaNotaRestaurante);
        resumo.setTotalAvaliacoesRestaurante(totalAvaliacoesRestaurante);
        return resumo;
    }
    
    public AvaliacaoResumoEntregadorDTO toResumoEntregadorDTO(BigDecimal mediaNotaEntregador, long totalAvaliacoesEntregador) {
        AvaliacaoResumoEntregadorDTO resumo = new AvaliacaoResumoEntregadorDTO();
        resumo.setMediaNotaEntregador(mediaNotaEntregador);
        resumo.setTotalAvaliacoesEntregador(totalAvaliacoesEntregador);
        return resumo;
    }
}

