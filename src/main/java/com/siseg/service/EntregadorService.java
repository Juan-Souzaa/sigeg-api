package com.siseg.service;

import com.siseg.dto.entregador.EntregadorRequestDTO;
import com.siseg.dto.entregador.EntregadorResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Entregador;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import com.siseg.util.SecurityUtils;
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

    public EntregadorService(EntregadorRepository entregadorRepository, UserRepository userRepository,
                            RoleRepository roleRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.entregadorRepository = entregadorRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    public EntregadorResponseDTO criarEntregador(EntregadorRequestDTO dto) {
        // Verificar se já existe usuário com este email
        if (userRepository.findByUsername(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com este email.");
        }

        // Verificar se já existe CPF cadastrado
        if (entregadorRepository.findByCpf(dto.getCpf()).isPresent()) {
            throw new IllegalArgumentException("Já existe um entregador com este CPF.");
        }

        // Criar User primeiro
        User user = new User();
        user.setUsername(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Adicionar role de entregador
        Set<Role> roles = new HashSet<>();
        Role entregadorRole = roleRepository.findByRoleName(ERole.ROLE_ENTREGADOR)
                .orElseThrow(() -> new ResourceNotFoundException("Role ENTREGADOR não encontrada"));
        roles.add(entregadorRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Criar Entregador
        Entregador entregador = modelMapper.map(dto, Entregador.class);
        entregador.setUser(savedUser);
        entregador.setStatus(StatusEntregador.PENDING_APPROVAL);
        entregador.setCriadoEm(Instant.now());

        Entregador saved = entregadorRepository.save(entregador);

        // Log de notificação (simulação - inicialmente via logs)
        logger.info(String.format("NOTIFICAÇÃO SIMULADA: Email enviado para %s - Entregador cadastrado com status PENDING_APPROVAL", saved.getEmail()));

        EntregadorResponseDTO response = modelMapper.map(saved, EntregadorResponseDTO.class);
        response.setUserId(savedUser.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public EntregadorResponseDTO buscarPorId(Long id) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com ID: " + id));
        
        validateEntregadorOwnership(entregador);
        
        EntregadorResponseDTO response = modelMapper.map(entregador, EntregadorResponseDTO.class);
        response.setUserId(entregador.getUser().getId());
        return response;
    }

    @Transactional(readOnly = true)
    public Page<EntregadorResponseDTO> listarTodos(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Admin pode ver todos os entregadores
        if (SecurityUtils.isAdmin()) {
            Page<Entregador> entregadores = entregadorRepository.findAll(pageable);
            return entregadores.map(e -> {
                EntregadorResponseDTO dto = modelMapper.map(e, EntregadorResponseDTO.class);
                dto.setUserId(e.getUser().getId());
                return dto;
            });
        }
        
        // Entregador só vê seus próprios dados
        Entregador entregador = entregadorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado para o usuário autenticado"));
        
        EntregadorResponseDTO dto = modelMapper.map(entregador, EntregadorResponseDTO.class);
        dto.setUserId(entregador.getUser().getId());
        return new PageImpl<>(List.of(dto), pageable, 1);
    }

    @Transactional(readOnly = true)
    public Page<EntregadorResponseDTO> findByStatus(StatusEntregador status, Pageable pageable) {
        Page<Entregador> entregadores = entregadorRepository.findByStatus(status, pageable);
        return entregadores.map(e -> {
            EntregadorResponseDTO dto = modelMapper.map(e, EntregadorResponseDTO.class);
            dto.setUserId(e.getUser().getId());
            return dto;
        });
    }

    public EntregadorResponseDTO aprovarEntregador(Long id) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com ID: " + id));
        
        entregador.setStatus(StatusEntregador.APPROVED);
        entregador.setAtualizadoEm(Instant.now());
        Entregador saved = entregadorRepository.save(entregador);
        
        // Log de notificação (simulação)
        logger.info(String.format("NOTIFICAÇÃO SIMULADA: Email enviado para %s - Entregador aprovado", saved.getEmail()));
        
        EntregadorResponseDTO response = modelMapper.map(saved, EntregadorResponseDTO.class);
        response.setUserId(saved.getUser().getId());
        return response;
    }

    public EntregadorResponseDTO rejeitarEntregador(Long id) {
        Entregador entregador = entregadorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entregador não encontrado com ID: " + id));
        
        entregador.setStatus(StatusEntregador.REJECTED);
        entregador.setAtualizadoEm(Instant.now());
        Entregador saved = entregadorRepository.save(entregador);
        
        // Log de notificação (simulação)
        logger.info(String.format("NOTIFICAÇÃO SIMULADA: Email enviado para %s - Entregador rejeitado", saved.getEmail()));
        
        EntregadorResponseDTO response = modelMapper.map(saved, EntregadorResponseDTO.class);
        response.setUserId(saved.getUser().getId());
        return response;
    }

    private void validateEntregadorOwnership(Entregador entregador) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Admin pode acessar qualquer entregador
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        // Verifica se o entregador pertence ao usuário autenticado
        if (entregador.getUser() == null || !entregador.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Você não tem permissão para acessar este entregador");
        }
    }
}

