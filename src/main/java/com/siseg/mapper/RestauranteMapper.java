package com.siseg.mapper;

import com.siseg.dto.restaurante.RestauranteBuscaDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Restaurante;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.service.EnderecoService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RestauranteMapper {
    
    private final AvaliacaoRepository avaliacaoRepository;
    private final ModelMapper modelMapper;
    private final EnderecoService enderecoService;
    
    public RestauranteMapper(AvaliacaoRepository avaliacaoRepository, ModelMapper modelMapper, EnderecoService enderecoService) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.modelMapper = modelMapper;
        this.enderecoService = enderecoService;
    }
    
    public RestauranteResponseDTO toResponseDTO(Restaurante restaurante) {
        RestauranteResponseDTO dto = modelMapper.map(restaurante, RestauranteResponseDTO.class);
        enderecoService.buscarEnderecoPrincipalRestaurante(restaurante.getId())
            .ifPresentOrElse(
                endereco -> dto.setEndereco(endereco.toGeocodingString()),
                () -> dto.setEndereco(null)
            );
        if (restaurante.getRaioEntregaKm() == null) {
            dto.setRaioEntregaKm(new BigDecimal("10.00"));
        }
        return dto;
    }
    
    public RestauranteBuscaDTO toRestauranteBuscaDTO(Restaurante restaurante, Cliente cliente) {
        RestauranteBuscaDTO dto = new RestauranteBuscaDTO();
        dto.setId(restaurante.getId());
        dto.setNome(restaurante.getNome());
        
        // Usar endereço principal do restaurante
        restaurante.getEnderecoPrincipal()
                .ifPresentOrElse(
                    endereco -> dto.setEndereco(endereco.toGeocodingString()),
                    () -> dto.setEndereco("Endereço não disponível")
                );
        
        dto.setTelefone(restaurante.getTelefone());
        
        BigDecimal raioEntrega = restaurante.getRaioEntregaKm();
        if (raioEntrega == null) {
            raioEntrega = new BigDecimal("10.00");
        }
        dto.setRaioEntregaKm(raioEntrega);
        
        BigDecimal mediaAvaliacao = avaliacaoRepository.calcularMediaNotaRestaurante(restaurante.getId());
        long totalAvaliacoes = avaliacaoRepository.countByRestauranteId(restaurante.getId());
        
        if (mediaAvaliacao != null) {
            dto.setMediaAvaliacao(mediaAvaliacao);
            dto.setTotalAvaliacoes(totalAvaliacoes);
        }
        
        return dto;
    }
}

