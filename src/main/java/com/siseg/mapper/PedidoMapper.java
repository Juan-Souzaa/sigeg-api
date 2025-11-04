package com.siseg.mapper;

import com.siseg.dto.cardapio.CardapioResponseDTO;
import com.siseg.dto.cardapio.CategoriaCardapioDTO;
import com.siseg.dto.cardapio.PratoCardapioDTO;
import com.siseg.dto.entregador.EntregadorSimplesDTO;
import com.siseg.dto.pedido.PedidoItemResponseDTO;
import com.siseg.dto.pedido.PedidoResponseDTO;
import com.siseg.model.Pedido;
import com.siseg.model.Prato;
import com.siseg.model.enumerations.CategoriaMenu;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PedidoMapper {
    
    private final ModelMapper modelMapper;
    
    public PedidoMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }
    
    public PedidoResponseDTO toResponseDTO(Pedido pedido) {
        PedidoResponseDTO response = modelMapper.map(pedido, PedidoResponseDTO.class);
        response.setClienteId(pedido.getCliente().getId());
        response.setRestauranteId(pedido.getRestaurante().getId());
        
        if (pedido.getEntregador() != null) {
            response.setEntregador(toEntregadorSimplesDTO(pedido));
        }
        
        response.setTempoEstimadoEntrega(pedido.getTempoEstimadoEntrega());
        response.setItens(toPedidoItemResponseDTOList(pedido));
        
        return response;
    }
    
    private EntregadorSimplesDTO toEntregadorSimplesDTO(Pedido pedido) {
        EntregadorSimplesDTO dto = new EntregadorSimplesDTO();
        dto.setId(pedido.getEntregador().getId());
        dto.setNome(pedido.getEntregador().getNome());
        dto.setTelefone(pedido.getEntregador().getTelefone());
        return dto;
    }
    
    private List<PedidoItemResponseDTO> toPedidoItemResponseDTOList(Pedido pedido) {
        return pedido.getItens().stream()
                .map(item -> {
                    PedidoItemResponseDTO itemDto = modelMapper.map(item, PedidoItemResponseDTO.class);
                    itemDto.setPratoId(item.getPrato().getId());
                    itemDto.setPratoNome(item.getPrato().getNome());
                    return itemDto;
                })
                .toList();
    }
    
    public CardapioResponseDTO toCardapioResponseDTO(Long restauranteId, String restauranteNome, 
                                                      Map<CategoriaMenu, List<Prato>> pratosPorCategoria) {
        CardapioResponseDTO response = new CardapioResponseDTO();
        response.setRestauranteId(restauranteId);
        response.setRestauranteNome(restauranteNome);
        
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
        return response;
    }
}

