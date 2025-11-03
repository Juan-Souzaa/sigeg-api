package com.siseg.service;

import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.RestauranteRepository;
import com.siseg.util.SecurityUtils;
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
    private final GeocodingService geocodingService;
    
    public RestauranteService(RestauranteRepository restauranteRepository, ModelMapper modelMapper, GeocodingService geocodingService) {
        this.restauranteRepository = restauranteRepository;
        this.modelMapper = modelMapper;
        this.geocodingService = geocodingService;
    }
    
    public RestauranteResponseDTO criarRestaurante(RestauranteRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Restaurante restaurante = modelMapper.map(dto, Restaurante.class);
        restaurante.setStatus(StatusRestaurante.PENDING_APPROVAL);
        restaurante.setUser(currentUser);
        
        // Geocodificar endereço automaticamente
        geocodingService.geocodeAddress(restaurante.getEndereco())
                .ifPresentOrElse(
                    coords -> {
                        restaurante.setLatitude(coords.getLatitude());
                        restaurante.setLongitude(coords.getLongitude());
                        logger.info("Coordenadas geocodificadas para restaurante: " + restaurante.getEndereco());
                    },
                    () -> logger.warning("Não foi possível geocodificar endereço do restaurante: " + restaurante.getEndereco())
                );
        
        Restaurante saved = restauranteRepository.save(restaurante);
        
        // Simular envio de email
        logger.info("Email simulado enviado para: " + saved.getEmail() + " - Status: PENDING_APPROVAL");
        
        return modelMapper.map(saved, RestauranteResponseDTO.class);
    }
    
    private void validateRestauranteOwnership(Restaurante restaurante) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Admin pode acessar qualquer restaurante
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        // Verifica se o restaurante pertence ao usuário autenticado
        if (restaurante.getUser() == null || !restaurante.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Você não tem permissão para acessar este restaurante");
        }
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
