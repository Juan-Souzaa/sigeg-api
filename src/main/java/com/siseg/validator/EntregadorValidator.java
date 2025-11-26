package com.siseg.validator;

import com.siseg.model.Entregador;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class EntregadorValidator {
    
    private final UserRepository userRepository;
    private final EntregadorRepository entregadorRepository;
    
    public EntregadorValidator(UserRepository userRepository, EntregadorRepository entregadorRepository) {
        this.userRepository = userRepository;
        this.entregadorRepository = entregadorRepository;
    }
    
    public void validateEmailUnico(String email) {
        if (userRepository.findByUsername(email).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com este email.");
        }
    }
    
    public void validateCpfUnico(String cpf) {
        if (entregadorRepository.findByCpf(cpf).isPresent()) {
            throw new IllegalArgumentException("Já existe um entregador com este CPF.");
        }
    }
    
    public void validateEntregadorAprovado(Entregador entregador) {
        if (entregador.getStatus() != StatusEntregador.APPROVED) {
            throw new IllegalStateException("Entregador deve estar aprovado para alterar disponibilidade");
        }
    }
}

