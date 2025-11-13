package com.siseg.service;

import com.siseg.dto.restaurante.RestauranteBuscaDTO;
import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.mapper.RestauranteMapper;
import com.siseg.model.Cliente;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.util.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Logger;

@Service
@Transactional
public class RestauranteService {
    
    private static final Logger logger = Logger.getLogger(RestauranteService.class.getName());
    
    private final RestauranteRepository restauranteRepository;
    private final ModelMapper modelMapper;
    private final EnderecoService enderecoService;
    private final ClienteRepository clienteRepository;
    private final RestauranteMapper restauranteMapper;
    
    public RestauranteService(RestauranteRepository restauranteRepository, ModelMapper modelMapper, 
                              EnderecoService enderecoService, 
                              ClienteRepository clienteRepository, RestauranteMapper restauranteMapper) {
        this.restauranteRepository = restauranteRepository;
        this.modelMapper = modelMapper;
        this.enderecoService = enderecoService;
        this.clienteRepository = clienteRepository;
        this.restauranteMapper = restauranteMapper;
    }
    
    public RestauranteResponseDTO criarRestaurante(RestauranteRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Restaurante restaurante = modelMapper.map(dto, Restaurante.class);
        restaurante.setStatus(StatusRestaurante.PENDING_APPROVAL);
        restaurante.setUser(currentUser);
        
        Restaurante saved = restauranteRepository.save(restaurante);
        
        enderecoService.criarEndereco(dto.getEndereco(), saved);
        
        logger.info("Email simulado enviado para: " + saved.getEmail() + " - Status: PENDING_APPROVAL");
        
        Restaurante restauranteComEnderecos = restauranteRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + saved.getId()));
        
        return restauranteMapper.toResponseDTO(restauranteComEnderecos);
    }
    
    public RestauranteResponseDTO buscarPorId(Long id) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        return restauranteMapper.toResponseDTO(restaurante);
    }
    
    public RestauranteResponseDTO aprovarRestaurante(Long id) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        
        restaurante.setStatus(StatusRestaurante.APPROVED);
        Restaurante saved = restauranteRepository.save(restaurante);
        
        logger.info("Restaurante aprovado: " + saved.getNome() + " - Email: " + saved.getEmail());
        
        return restauranteMapper.toResponseDTO(saved);
    }
    
    public RestauranteResponseDTO rejeitarRestaurante(Long id) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        
        restaurante.setStatus(StatusRestaurante.REJECTED);
        Restaurante saved = restauranteRepository.save(restaurante);
        
        logger.info("Restaurante rejeitado: " + saved.getNome() + " - Email: " + saved.getEmail());
        
        return restauranteMapper.toResponseDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public Page<RestauranteResponseDTO> listarTodos(Pageable pageable) {
        Page<Restaurante> restaurantes = restauranteRepository.findAll(pageable);
        return restaurantes.map(restauranteMapper::toResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<RestauranteResponseDTO> listarPorStatus(StatusRestaurante status, Pageable pageable) {
        Page<Restaurante> restaurantes = restauranteRepository.findByStatus(status, pageable);
        return restaurantes.map(restauranteMapper::toResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<RestauranteBuscaDTO> buscarRestaurantes(String cozinha, Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = clienteRepository.findByUserId(currentUser.getId())
                .orElse(null);
        
        Page<Restaurante> restaurantes = restauranteRepository.buscarRestaurantesAprovados(cozinha, pageable);
        
        List<RestauranteBuscaDTO> dtos = restaurantes.getContent().stream()
                .map(r -> restauranteMapper.toRestauranteBuscaDTO(r, cliente))
                .toList();
        
        return new PageImpl<>(dtos, pageable, restaurantes.getTotalElements());
    }
}
