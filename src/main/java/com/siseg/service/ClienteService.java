package com.siseg.service;

import com.siseg.dto.cliente.ClienteRequestDTO;
import com.siseg.dto.cliente.ClienteResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import com.siseg.util.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final EnderecoService enderecoService;

    public ClienteService(ClienteRepository clienteRepository, UserRepository userRepository, 
                         RoleRepository roleRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper,
                         EnderecoService enderecoService) {
        this.clienteRepository = clienteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.enderecoService = enderecoService;
    }

    public ClienteResponseDTO criarCliente(ClienteRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        
        Set<Role> roles = new HashSet<>();
        Role clienteRole = roleRepository.findByRoleName(ERole.ROLE_CLIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("Role CLIENTE não encontrado"));
        roles.add(clienteRole);
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        
        Cliente cliente = modelMapper.map(dto, Cliente.class);
        cliente.setUser(savedUser);
        
        Cliente saved = clienteRepository.save(cliente);
        
        enderecoService.criarEndereco(dto.getEndereco(), saved);
        
        Cliente clienteComEnderecos = clienteRepository.findById(saved.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + saved.getId()));
        
        return toResponseDTO(clienteComEnderecos);
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        
        validateClienteOwnership(cliente);
        
        return toResponseDTO(cliente);
    }

    public Page<ClienteResponseDTO> listarTodos(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        if (SecurityUtils.isAdmin()) {
            Page<Cliente> clientes = clienteRepository.findAll(pageable);
            return clientes.map(this::toResponseDTO);
        }
        
        Page<Cliente> clientes = clienteRepository.findByUserId(currentUser.getId(), pageable);
        return clientes.map(this::toResponseDTO);
    }
    
    private ClienteResponseDTO toResponseDTO(Cliente cliente) {
        ClienteResponseDTO dto = modelMapper.map(cliente, ClienteResponseDTO.class);
        enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())
            .ifPresentOrElse(
                endereco -> dto.setEndereco(endereco.toGeocodingString()),
                () -> dto.setEndereco(null)
            );
        return dto;
    }
    
    private void validateClienteOwnership(Cliente cliente) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        if (cliente.getUser() == null || !cliente.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Você não tem permissão para acessar este cliente");
        }
    }
}
