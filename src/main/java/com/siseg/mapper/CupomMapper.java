package com.siseg.mapper;

import com.siseg.dto.cupom.CupomResponseDTO;
import com.siseg.model.Cupom;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CupomMapper {

    private final ModelMapper modelMapper;

    public CupomMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public CupomResponseDTO toResponseDTO(Cupom cupom) {
        return modelMapper.map(cupom, CupomResponseDTO.class);
    }
}

