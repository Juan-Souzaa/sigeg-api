package com.siseg.mapper;

import com.siseg.dto.EnderecoResponseDTO;
import com.siseg.model.Endereco;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class EnderecoMapper {
    
    private final ModelMapper modelMapper;
    
    public EnderecoMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }
    
    public EnderecoResponseDTO toResponseDTO(Endereco endereco) {
        EnderecoResponseDTO dto = modelMapper.map(endereco, EnderecoResponseDTO.class);
        if (endereco.getCep() != null) {
            dto.setCep(endereco.formatarCep());
        }
        return dto;
    }
}

