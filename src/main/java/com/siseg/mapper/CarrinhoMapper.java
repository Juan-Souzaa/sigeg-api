package com.siseg.mapper;

import com.siseg.dto.carrinho.CarrinhoItemResponseDTO;
import com.siseg.dto.carrinho.CarrinhoResponseDTO;
import com.siseg.dto.carrinho.CupomInfoDTO;
import com.siseg.model.Carrinho;
import com.siseg.model.CarrinhoItem;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CarrinhoMapper {

    private final ModelMapper modelMapper;

    public CarrinhoMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public CarrinhoResponseDTO toResponseDTO(Carrinho carrinho) {
        CarrinhoResponseDTO dto = modelMapper.map(carrinho, CarrinhoResponseDTO.class);
        dto.setClienteId(carrinho.getCliente().getId());
        dto.setItens(toItemResponseDTOList(carrinho.getItens()));
        dto.setCupom(toCupomInfoDTO(carrinho.getCupom()));
        return dto;
    }

    public CarrinhoItemResponseDTO toItemResponseDTO(CarrinhoItem item) {
        CarrinhoItemResponseDTO dto = modelMapper.map(item, CarrinhoItemResponseDTO.class);
        dto.setPratoId(item.getPrato().getId());
        dto.setPratoNome(item.getPrato().getNome());
        return dto;
    }

    public List<CarrinhoItemResponseDTO> toItemResponseDTOList(List<CarrinhoItem> itens) {
        return itens.stream()
                .map(this::toItemResponseDTO)
                .toList();
    }

    private CupomInfoDTO toCupomInfoDTO(com.siseg.model.Cupom cupom) {
        if (cupom == null) {
            return null;
        }
        CupomInfoDTO dto = new CupomInfoDTO();
        dto.setId(cupom.getId());
        dto.setCodigo(cupom.getCodigo());
        dto.setTipoDesconto(cupom.getTipoDesconto().name());
        dto.setValorDesconto(cupom.getValorDesconto());
        return dto;
    }
}

