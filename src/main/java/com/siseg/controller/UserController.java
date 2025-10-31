package com.siseg.controller;

import com.siseg.dto.authentication.UserRegistrationDTO;
import com.siseg.exception.UserAlreadyExistsException;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody UserRegistrationDTO dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        Role roleUser = roleRepository.findByRoleName(ERole.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found"));

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.getRoles().add(roleUser);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}


