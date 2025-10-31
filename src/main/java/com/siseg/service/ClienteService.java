package com.siseg.service;

import com.siseg.dto.cliente.ClienteRequestDTO;
import com.siseg.dto.cliente.ClienteResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
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

    public ClienteService(ClienteRepository clienteRepository, UserRepository userRepository, 
                         RoleRepository roleRepository, PasswordEncoder passwordEncoder, ModelMapper modelMapper) {
        this.clienteRepository = clienteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    public ClienteResponseDTO criarCliente(ClienteRequestDTO dto) {
        // Criar User primeiro
        User user = new User();
        user.setUsername(dto.getEmail()); // Usar email como username
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        
        // Adicionar role de cliente
        Set<Role> roles = new HashSet<>();
        Role clienteRole = roleRepository.findByRoleName(ERole.ROLE_CLIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("Role CLIENTE não encontrado"));
        roles.add(clienteRole);
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        
        // Criar Cliente
        Cliente cliente = modelMapper.map(dto, Cliente.class);
        cliente.setUser(savedUser);
        Cliente saved = clienteRepository.save(cliente);
        
        return modelMapper.map(saved, ClienteResponseDTO.class);
    }

    public ClienteResponseDTO buscarPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        return modelMapper.map(cliente, ClienteResponseDTO.class);
    }

    public Page<ClienteResponseDTO> listarTodos(Pageable pageable) {
        Page<Cliente> clientes = clienteRepository.findAll(pageable);
        return clientes.map(cliente -> modelMapper.map(cliente, ClienteResponseDTO.class));
    }
}
