package com.siseg.service;

import com.siseg.dto.AtualizarSenhaDTO;
import com.siseg.dto.restaurante.RestauranteBuscaDTO;
import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.dto.restaurante.RestauranteUpdateDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.exception.UserAlreadyExistsException;
import com.siseg.mapper.RestauranteMapper;
import com.siseg.model.Cliente;
import com.siseg.model.Restaurante;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.PratoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import com.siseg.util.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.util.TempoEstimadoCalculator;
import com.siseg.model.Endereco;
import com.siseg.model.enumerations.TipoVeiculo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
public class RestauranteService {
    
    private static final Logger logger = Logger.getLogger(RestauranteService.class.getName());
    
    private final RestauranteRepository restauranteRepository;
    private final ModelMapper modelMapper;
    private final EnderecoService enderecoService;
    private final ClienteRepository clienteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PedidoRepository pedidoRepository;
    private final PratoRepository pratoRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestauranteMapper restauranteMapper;
    private final TempoEstimadoCalculator tempoEstimadoCalculator;
    
    public RestauranteService(RestauranteRepository restauranteRepository, ModelMapper modelMapper, 
                              EnderecoService enderecoService, 
                              ClienteRepository clienteRepository, UserRepository userRepository,
                              RoleRepository roleRepository, PedidoRepository pedidoRepository, 
                              PratoRepository pratoRepository, PasswordEncoder passwordEncoder, 
                              RestauranteMapper restauranteMapper, TempoEstimadoCalculator tempoEstimadoCalculator) {
        this.restauranteRepository = restauranteRepository;
        this.modelMapper = modelMapper;
        this.enderecoService = enderecoService;
        this.clienteRepository = clienteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.pedidoRepository = pedidoRepository;
        this.pratoRepository = pratoRepository;
        this.passwordEncoder = passwordEncoder;
        this.restauranteMapper = restauranteMapper;
        this.tempoEstimadoCalculator = tempoEstimadoCalculator;
    }
    
    public RestauranteResponseDTO criarRestaurante(RestauranteRequestDTO dto) {
        if (userRepository.findByUsername(dto.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Já existe um usuário com este email.");
        }
        
        if (restauranteRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Já existe um restaurante com este email.");
        }
        
        User user = new User();
        user.setUsername(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        
        Set<Role> roles = new HashSet<>();
        Role restauranteRole = roleRepository.findByRoleName(ERole.ROLE_RESTAURANTE)
                .orElseThrow(() -> new ResourceNotFoundException("Role RESTAURANTE não encontrado"));
        roles.add(restauranteRole);
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        
        Restaurante restaurante = modelMapper.map(dto, Restaurante.class);
        restaurante.setStatus(StatusRestaurante.PENDING_APPROVAL);
        restaurante.setUser(savedUser);
        
        if (dto.getRaioEntregaKm() != null) {
            restaurante.setRaioEntregaKm(dto.getRaioEntregaKm());
        } else {
            restaurante.setRaioEntregaKm(new BigDecimal("10.00"));
        }
        
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
    
    public RestauranteResponseDTO atualizarRestaurante(Long id, RestauranteUpdateDTO dto) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        
        SecurityUtils.validateRestauranteOwnership(restaurante);
        
        if (!restaurante.getEmail().equals(dto.getEmail())) {
            if (restauranteRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("Já existe um restaurante com este email.");
            }
        }
        
        restaurante.setNome(dto.getNome());
        restaurante.setEmail(dto.getEmail());
        restaurante.setTelefone(dto.getTelefone());
        
        if (dto.getRaioEntregaKm() != null) {
            restaurante.setRaioEntregaKm(dto.getRaioEntregaKm());
        }
        
        if (restaurante.getUser() != null) {
            restaurante.getUser().setUsername(dto.getEmail());
            userRepository.save(restaurante.getUser());
        }
        
        Restaurante saved = restauranteRepository.save(restaurante);
        return restauranteMapper.toResponseDTO(saved);
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
        Cliente cliente = buscarClienteAutenticado();
        List<Restaurante> restaurantesAprovados = buscarRestaurantesAprovados(cozinha);
        Optional<Endereco> enderecoCliente = buscarEnderecoCliente(cliente);
        
        List<RestauranteBuscaDTO> dtos = processarRestaurantes(restaurantesAprovados, cliente, enderecoCliente);
        List<RestauranteBuscaDTO> ordenados = ordenarPorDistancia(dtos);
        
        return aplicarPaginacao(ordenados, pageable);
    }
    
    private Cliente buscarClienteAutenticado() {
        User currentUser = SecurityUtils.getCurrentUser();
        return clienteRepository.findByUserId(currentUser.getId()).orElse(null);
    }
    
    private List<Restaurante> buscarRestaurantesAprovados(String cozinha) {
        return restauranteRepository.findAll().stream()
                .filter(r -> r.getStatus() == StatusRestaurante.APPROVED && Boolean.TRUE.equals(r.getAtivo()))
                .filter(r -> filtrarPorCozinha(r, cozinha))
                .collect(Collectors.toList());
    }
    
    private boolean filtrarPorCozinha(Restaurante restaurante, String cozinha) {
        return cozinha == null || cozinha.isEmpty() || 
               restaurante.getNome().toLowerCase().contains(cozinha.toLowerCase());
    }
    
    private Optional<Endereco> buscarEnderecoCliente(Cliente cliente) {
        if (cliente == null) {
            return Optional.empty();
        }
        return enderecoService.buscarEnderecoPrincipalCliente(cliente.getId());
    }
    
    private List<RestauranteBuscaDTO> processarRestaurantes(List<Restaurante> restaurantes, 
                                                             Cliente cliente, 
                                                             Optional<Endereco> enderecoCliente) {
        List<RestauranteBuscaDTO> dtos = new ArrayList<>();
        BigDecimal raioPadrao = new BigDecimal("10.00");
        
        for (Restaurante restaurante : restaurantes) {
            if (deveIncluirRestaurante(restaurante, enderecoCliente, raioPadrao)) {
                RestauranteBuscaDTO dto = criarDTOComDistancia(restaurante, cliente, enderecoCliente);
                dtos.add(dto);
            }
        }
        
        return dtos;
    }
    
    private boolean deveIncluirRestaurante(Restaurante restaurante, 
                                            Optional<Endereco> enderecoCliente, 
                                            BigDecimal raioPadrao) {
        if (!enderecoCliente.isPresent()) {
            return true;
        }
        
        Optional<Endereco> enderecoRestaurante = enderecoService.buscarEnderecoPrincipalRestaurante(restaurante.getId());
        if (!enderecoRestaurante.isPresent()) {
            return true;
        }
        
        Endereco endCliente = enderecoCliente.get();
        Endereco endRestaurante = enderecoRestaurante.get();
        
        if (!temCoordenadasValidas(endCliente) || !temCoordenadasValidas(endRestaurante)) {
            return true;
        }
        
        ResultadoCalculo resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
            endCliente.getLatitude(), endCliente.getLongitude(),
            endRestaurante.getLatitude(), endRestaurante.getLongitude(),
            TipoVeiculo.MOTO
        );
        
        if (resultado == null || resultado.getDistanciaKm() == null) {
            return true;
        }
        
        BigDecimal raioRestaurante = obterRaioEntrega(restaurante, raioPadrao);
        return resultado.getDistanciaKm().compareTo(raioRestaurante) <= 0;
    }
    
    private boolean temCoordenadasValidas(Endereco endereco) {
        if (endereco.getLatitude() == null || endereco.getLongitude() == null) {
            return false;
        }
        return true;
        
     
    }
    
    private BigDecimal obterRaioEntrega(Restaurante restaurante, BigDecimal raioPadrao) {
        return restaurante.getRaioEntregaKm() != null ? restaurante.getRaioEntregaKm() : raioPadrao;
    }
    
    private RestauranteBuscaDTO criarDTOComDistancia(Restaurante restaurante, 
                                                       Cliente cliente, 
                                                       Optional<Endereco> enderecoCliente) {
        RestauranteBuscaDTO dto = restauranteMapper.toRestauranteBuscaDTO(restaurante, cliente);
        
        if (enderecoCliente.isPresent()) {
            Optional<Endereco> enderecoRestaurante = enderecoService.buscarEnderecoPrincipalRestaurante(restaurante.getId());
            if (enderecoRestaurante.isPresent()) {
                Endereco endCliente = enderecoCliente.get();
                Endereco endRestaurante = enderecoRestaurante.get();
                
                if (temCoordenadasValidas(endCliente) && temCoordenadasValidas(endRestaurante)) {
                    ResultadoCalculo resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
                        endCliente.getLatitude(), endCliente.getLongitude(),
                        endRestaurante.getLatitude(), endRestaurante.getLongitude(),
                        TipoVeiculo.MOTO
                    );
                    
                    if (resultado != null && resultado.getDistanciaKm() != null) {
                        dto.setDistanciaKm(resultado.getDistanciaKm());
                    }
                    
                    if (resultado != null && resultado.getTempoMinutos() > 0) {
                        dto.setTempoEstimadoMinutos(resultado.getTempoMinutos());
                    }
                }
            }
        }
        
        return dto;
    }
    
    private List<RestauranteBuscaDTO> ordenarPorDistancia(List<RestauranteBuscaDTO> dtos) {
        BigDecimal distanciaMaxima = new BigDecimal("999999");
        
        dtos.sort(Comparator
                .comparing((RestauranteBuscaDTO dto) -> 
                    dto.getDistanciaKm() != null ? dto.getDistanciaKm() : distanciaMaxima)
                .thenComparing(RestauranteBuscaDTO::getNome));
        
        return dtos;
    }
    
    private Page<RestauranteBuscaDTO> aplicarPaginacao(List<RestauranteBuscaDTO> dtos, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dtos.size());
        List<RestauranteBuscaDTO> paginatedDtos = start < dtos.size() ? 
                dtos.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(paginatedDtos, pageable, dtos.size());
    }
    
    public void atualizarRaioEntrega(Long id, BigDecimal raioEntregaKm) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        
        SecurityUtils.validateRestauranteOwnership(restaurante);
        
        if (raioEntregaKm.compareTo(new BigDecimal("0.1")) < 0) {
            throw new IllegalArgumentException("Raio de entrega deve ser no mínimo 0.1 km");
        }
        
        if (raioEntregaKm.compareTo(new BigDecimal("50.0")) > 0) {
            throw new IllegalArgumentException("Raio de entrega não pode ser maior que 50 km");
        }
        
        restaurante.setRaioEntregaKm(raioEntregaKm);
        restauranteRepository.save(restaurante);
    }
    
    public void excluirRestaurante(Long id) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        
        SecurityUtils.validateRestauranteOwnership(restaurante);
        
        List<StatusPedido> statusesEmAndamento = List.of(
            StatusPedido.CREATED,
            StatusPedido.CONFIRMED,
            StatusPedido.PREPARING,
            StatusPedido.OUT_FOR_DELIVERY
        );
        
        boolean temPedidosEmAndamento = pedidoRepository.existsByRestauranteIdAndStatusIn(id, statusesEmAndamento);
        
        if (temPedidosEmAndamento) {
            throw new IllegalStateException("Não é possível excluir restaurante com pedidos em andamento");
        }
        
        long pratosAtivos = pratoRepository.findByRestauranteId(id, Pageable.unpaged())
                .getContent().stream()
                .filter(p -> Boolean.TRUE.equals(p.getDisponivel()))
                .count();
        
        if (pratosAtivos > 0) {
            throw new IllegalStateException("Não é possível excluir restaurante com pratos ativos");
        }
        
        restaurante.setAtivo(false);
        restauranteRepository.save(restaurante);
    }
    
    public void atualizarSenha(Long id, AtualizarSenhaDTO dto) {
        Restaurante restaurante = restauranteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));
        
        SecurityUtils.validateRestauranteOwnership(restaurante);
        
        if (restaurante.getUser() == null) {
            throw new ResourceNotFoundException("Usuário não encontrado para o restaurante");
        }
        
        if (!passwordEncoder.matches(dto.getSenhaAtual(), restaurante.getUser().getPassword())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }
        
        restaurante.getUser().setPassword(passwordEncoder.encode(dto.getNovaSenha()));
        userRepository.save(restaurante.getUser());
    }
}
