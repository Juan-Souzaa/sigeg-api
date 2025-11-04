package com.siseg.mapper;

import com.siseg.dto.entregador.EntregadorResponseDTO;
import com.siseg.model.Entregador;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class EntregadorMapper {
    
    private final ModelMapper modelMapper;
    
    public EntregadorMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }
    
    public EntregadorResponseDTO toResponseDTO(Entregador entregador, Long userId) {
        EntregadorResponseDTO response = modelMapper.map(entregador, EntregadorResponseDTO.class);
        response.setUserId(userId);
        return response;
    }
}

