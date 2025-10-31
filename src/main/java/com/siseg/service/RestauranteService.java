package com.siseg.service;

import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.RestauranteRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.logging.Logger;

@Service
@Transactional
public class RestauranteService {
    
    private static final Logger logger = Logger.getLogger(RestauranteService.class.getName());
    
    private final RestauranteRepository restauranteRepository;
    private final ModelMapper modelMapper;
    
    public RestauranteService(RestauranteRepository restauranteRepository, ModelMapper modelMapper) {
        this.restauranteRepository = restauranteRepository;
        this.modelMapper = modelMapper;
    }
    
    public RestauranteResponseDTO criarRestaurante(RestauranteRequestDTO dto) {
        Restaurante restaurante = modelMapper.map(dto, Restaurante.class);
        restaurante.setStatus(StatusRestaurante.PENDING_APPROVAL);
        
        Restaurante saved = restauranteRepository.save(restaurante);
        
        // Simular envio de email
        logger.info("Email simulado enviado para: " + saved.getEmail() + " - Status: PENDING_APPROVAL");
        
        return modelMapper.map(saved, RestauranteResponseDTO.class);
    }
    
    public RestauranteResponseDTO buscarPorId(Long id) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        return modelMapper.map(restaurante, RestauranteResponseDTO.class);
    }
    
    public RestauranteResponseDTO aprovarRestaurante(Long id) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        
        restaurante.setStatus(StatusRestaurante.APPROVED);
        Restaurante saved = restauranteRepository.save(restaurante);
        
        logger.info("Restaurante aprovado: " + saved.getNome() + " - Email: " + saved.getEmail());
        
        return modelMapper.map(saved, RestauranteResponseDTO.class);
    }
    
    public RestauranteResponseDTO rejeitarRestaurante(Long id) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        
        restaurante.setStatus(StatusRestaurante.REJECTED);
        Restaurante saved = restauranteRepository.save(restaurante);
        
        logger.info("Restaurante rejeitado: " + saved.getNome() + " - Email: " + saved.getEmail());
        
        return modelMapper.map(saved, RestauranteResponseDTO.class);
    }
    
    @Transactional(readOnly = true)
    public Page<RestauranteResponseDTO> listarTodos(Pageable pageable) {
        Page<Restaurante> restaurantes = restauranteRepository.findAll(pageable);
        return restaurantes.map(r -> modelMapper.map(r, RestauranteResponseDTO.class));
    }
    
    @Transactional(readOnly = true)
    public Page<RestauranteResponseDTO> listarPorStatus(StatusRestaurante status, Pageable pageable) {
        Page<Restaurante> restaurantes = restauranteRepository.findByStatus(status, pageable);
        return restaurantes.map(r -> modelMapper.map(r, RestauranteResponseDTO.class));
    }
}
