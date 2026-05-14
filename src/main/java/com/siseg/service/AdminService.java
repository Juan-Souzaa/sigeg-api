package com.siseg.service;

import com.siseg.dto.admin.AdminRequestDTO;
import com.siseg.dto.admin.AdminResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.exception.UserAlreadyExistsException;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminResponseDTO> listarAdmins() {
        return userRepository.findAllByRoleName(ERole.ROLE_ADMIN).stream()
                .sorted(Comparator.comparing(User::getId))
                .map(user -> {
                    AdminResponseDTO dto = new AdminResponseDTO();
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    return dto;
                })
                .toList();
    }

    public AdminResponseDTO criarAdmin(AdminRequestDTO dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username já existe");
        }

        User admin = new User();
        admin.setUsername(dto.getUsername());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role adminRole = roleRepository.findByRoleName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role ADMIN não encontrado"));
        roles.add(adminRole);

        admin.setRoles(roles);
        User savedAdmin = userRepository.save(admin);

        AdminResponseDTO response = new AdminResponseDTO();
        response.setId(savedAdmin.getId());
        response.setUsername(savedAdmin.getUsername());

        return response;
    }
}

