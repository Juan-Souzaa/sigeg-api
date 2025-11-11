package com.siseg.service;

import com.siseg.dto.restaurante.RestauranteBuscaDTO;
import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.util.SecurityUtils;
import com.siseg.util.TempoEstimadoCalculator;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

@Service
@Transactional
public class RestauranteService {
    
    private static final Logger logger = Logger.getLogger(RestauranteService.class.getName());
    
    private final RestauranteRepository restauranteRepository;
    private final ModelMapper modelMapper;
    private final GeocodingService geocodingService;
    private final ClienteRepository clienteRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final TempoEstimadoCalculator tempoEstimadoCalculator;
    
    public RestauranteService(RestauranteRepository restauranteRepository, ModelMapper modelMapper, 
                              GeocodingService geocodingService, ClienteRepository clienteRepository,
                              AvaliacaoRepository avaliacaoRepository, TempoEstimadoCalculator tempoEstimadoCalculator) {
        this.restauranteRepository = restauranteRepository;
        this.modelMapper = modelMapper;
        this.geocodingService = geocodingService;
        this.clienteRepository = clienteRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.tempoEstimadoCalculator = tempoEstimadoCalculator;
    }
    
    public RestauranteResponseDTO criarRestaurante(RestauranteRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        Restaurante restaurante = modelMapper.map(dto, Restaurante.class);
        restaurante.setStatus(StatusRestaurante.PENDING_APPROVAL);
        restaurante.setUser(currentUser);
        
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
    
    @Transactional(readOnly = true)
    public Page<RestauranteBuscaDTO> buscarRestaurantes(String cozinha, Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        Cliente cliente = clienteRepository.findByUserId(currentUser.getId())
                .orElse(null);
        
        Page<Restaurante> restaurantes = restauranteRepository.buscarRestaurantesAprovados(cozinha, pageable);
        
        List<RestauranteBuscaDTO> dtos = restaurantes.getContent().stream()
                .map(r -> mapearParaRestauranteBuscaDTO(r, cliente))
                .toList();
        
        return new PageImpl<>(dtos, pageable, restaurantes.getTotalElements());
    }
    
    private RestauranteBuscaDTO mapearParaRestauranteBuscaDTO(Restaurante restaurante, Cliente cliente) {
        RestauranteBuscaDTO dto = new RestauranteBuscaDTO();
        dto.setId(restaurante.getId());
        dto.setNome(restaurante.getNome());
        dto.setEndereco(restaurante.getEndereco());
        dto.setTelefone(restaurante.getTelefone());
        
        if (cliente != null && cliente.getLatitude() != null && cliente.getLongitude() != null &&
            restaurante.getLatitude() != null && restaurante.getLongitude() != null) {
            var resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
                cliente.getLatitude(), cliente.getLongitude(),
                restaurante.getLatitude(), restaurante.getLongitude(),
                TipoVeiculo.MOTO
            );
            
            if (resultado.getDistanciaKm() != null) {
                dto.setDistanciaKm(resultado.getDistanciaKm());
            }
            
            if (resultado.getTempoMinutos() > 0) {
                dto.setTempoEstimadoMinutos(resultado.getTempoMinutos());
            }
        }
        
        BigDecimal mediaAvaliacao = avaliacaoRepository.calcularMediaNotaRestaurante(restaurante.getId());
        long totalAvaliacoes = avaliacaoRepository.countByRestauranteId(restaurante.getId());
        
        if (mediaAvaliacao != null) {
            dto.setMediaAvaliacao(mediaAvaliacao);
            dto.setTotalAvaliacoes(totalAvaliacoes);
        }
        
        return dto;
    }
}
