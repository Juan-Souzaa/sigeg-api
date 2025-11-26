package com.siseg.service;

import com.siseg.dto.entregador.EntregadorRequestDTO;
import com.siseg.dto.entregador.EntregadorResponseDTO;
import com.siseg.dto.entregador.EntregadorUpdateDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Entregador;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.DisponibilidadeEntregador;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import com.siseg.mapper.EntregadorMapper;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.EntregadorValidator;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Service
@Transactional
public class EntregadorService {

    private static final Logger logger = Logger.getLogger(EntregadorService.class.getName());

    private final EntregadorRepository entregadorRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final EntregadorMapper entregadorMapper;
    private final EntregadorValidator entregadorValidator;

    public EntregadorService(EntregadorRepository entregadorRepository, UserRepository userRepository,
                            RoleRepository roleRepository, PasswordEncoder passwordEncoder, 
                            ModelMapper modelMapper, EntregadorMapper entregadorMapper,
                            EntregadorValidator entregadorValidator) {
        this.entregadorRepository = entregadorRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.entregadorMapper = entregadorMapper;
        this.entregadorValidator = entregadorValidator;
    }

    public EntregadorResponseDTO criarEntregador(EntregadorRequestDTO dto) {
        entregadorValidator.validateEmailUnico(dto.getEmail());
        entregadorValidator.validateCpfUnico(dto.getCpf());
        
        User savedUser = criarUserEntregador(dto);
        Entregador saved = criarEntregadorBasico(dto, savedUser);
        
        logger.info(String.format("NOTIFICAÇÃO SIMULADA: Email enviado para %s - Entregador cadastrado com status PENDING_APPROVAL", saved.getEmail()));
        
        return entregadorMapper.toResponseDTO(saved, savedUser.getId());
    }
    
    private User criarUserEntregador(EntregadorRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRoles(obterRolesEntregador());
        return userRepository.save(user);
    }
    
    private Set<Role> obterRolesEntregador() {
        Set<Role> roles = new HashSet<>();
        Role entregadorRole = roleRepository.findByRoleName(ERole.ROLE_ENTREGADOR)
                .orElseThrow(() -> new ResourceNotFoundException("Role ENTREGADOR não encontrada"));
        roles.add(entregadorRole);
        return roles;
    }
    
    private Entregador criarEntregadorBasico(EntregadorRequestDTO dto, User user) {
        Entregador entregador = modelMapper.map(dto, Entregador.class);
        entregador.setUser(user);
        entregador.setStatus(StatusEntregador.PENDING_APPROVAL);
        entregador.setDisponibilidade(DisponibilidadeEntregador.UNAVAILABLE);
        entregador.setCriadoEm(Instant.now());
        return entregadorRepository.save(entregador);
    }
    

    @Transactional(readOnly = true)
    public EntregadorResponseDTO buscarPorId(Long id) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com ID: " + id));
        
        validateEntregadorOwnership(entregador);
        
        return entregadorMapper.toResponseDTO(entregador, entregador.getUser().getId());
    }

    @Transactional(readOnly = true)
    public Page<EntregadorResponseDTO> listarTodos(Pageable pageable) {
        if (SecurityUtils.isAdmin()) {
            return listarTodosEntregadores(pageable);
        }
        return listarProprioEntregador(pageable);
    }
    
    private Page<EntregadorResponseDTO> listarTodosEntregadores(Pageable pageable) {
        Page<Entregador> entregadores = entregadorRepository.findAll(pageable);
        return entregadores.map(e -> entregadorMapper.toResponseDTO(e, e.getUser().getId()));
    }
    
    private Page<EntregadorResponseDTO> listarProprioEntregador(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado para o usuário autenticado"));
        
        EntregadorResponseDTO dto = entregadorMapper.toResponseDTO(entregador, entregador.getUser().getId());
        return new PageImpl<>(List.of(dto), pageable, 1);
    }

    @Transactional(readOnly = true)
    public Page<EntregadorResponseDTO> findByStatus(StatusEntregador status, Pageable pageable) {
        Page<Entregador> entregadores = entregadorRepository.findByStatus(status, pageable);
        return entregadores.map(e -> entregadorMapper.toResponseDTO(e, e.getUser().getId()));
    }

    public EntregadorResponseDTO aprovarEntregador(Long id) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com ID: " + id));
        
        entregador.setStatus(StatusEntregador.APPROVED);
        
        entregador.setDisponibilidade(DisponibilidadeEntregador.UNAVAILABLE);
        entregador.setAtualizadoEm(Instant.now());
        Entregador saved = entregadorRepository.save(entregador);
        
        logger.info(String.format("NOTIFICAÇÃO SIMULADA: Email enviado para %s - Entregador aprovado", saved.getEmail()));
        
        return entregadorMapper.toResponseDTO(saved, saved.getUser().getId());
    }

    public EntregadorResponseDTO rejeitarEntregador(Long id) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com ID: " + id));
        
        entregador.setStatus(StatusEntregador.REJECTED);
        entregador.setAtualizadoEm(Instant.now());
        Entregador saved = entregadorRepository.save(entregador);
        
        logger.info(String.format("NOTIFICAÇÃO SIMULADA: Email enviado para %s - Entregador rejeitado", saved.getEmail()));
        
        return entregadorMapper.toResponseDTO(saved, saved.getUser().getId());
    }

    public EntregadorResponseDTO atualizarEntregador(Long id, EntregadorUpdateDTO dto) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com ID: " + id));
        
        validateEntregadorOwnership(entregador);
        
        atualizarCampos(entregador, dto);
        entregador.setAtualizadoEm(Instant.now());
        
        Entregador saved = entregadorRepository.save(entregador);
        return entregadorMapper.toResponseDTO(saved, saved.getUser().getId());
    }

    private void atualizarCampos(Entregador entregador, EntregadorUpdateDTO dto) {
        if (dto.getNome() != null) entregador.setNome(dto.getNome());
        if (dto.getTelefone() != null) entregador.setTelefone(dto.getTelefone());
        if (dto.getTipoVeiculo() != null) entregador.setTipoVeiculo(dto.getTipoVeiculo());
        if (dto.getPlacaVeiculo() != null) entregador.setPlacaVeiculo(dto.getPlacaVeiculo());
        if (dto.getLatitude() != null) entregador.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) entregador.setLongitude(dto.getLongitude());
        
        atualizarEmailSeNecessario(entregador, dto.getEmail());
    }

    private void atualizarEmailSeNecessario(Entregador entregador, String novoEmail) {
        if (novoEmail != null && !novoEmail.equals(entregador.getEmail())) {
            entregadorValidator.validateEmailUnico(novoEmail);
            entregador.setEmail(novoEmail);
            entregador.getUser().setUsername(novoEmail);
            userRepository.save(entregador.getUser());
        }
    }

    public EntregadorResponseDTO atualizarDisponibilidade(Long id, DisponibilidadeEntregador disponibilidade) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com ID: " + id));
        
        validateEntregadorOwnership(entregador);
        entregadorValidator.validateEntregadorAprovado(entregador);
        
        entregador.setDisponibilidade(disponibilidade);
        entregador.setAtualizadoEm(Instant.now());
        
        Entregador saved = entregadorRepository.save(entregador);
        return entregadorMapper.toResponseDTO(saved, saved.getUser().getId());
    }

    private void validateEntregadorOwnership(Entregador entregador) {
        SecurityUtils.validateEntregadorOwnership(entregador);
    }
}

