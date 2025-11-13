package com.siseg.validator;

import com.siseg.model.Endereco;
import org.springframework.stereotype.Component;

@Component
public class EnderecoValidator {
    
    public void validate(Endereco endereco) {
        if (endereco == null) {
            throw new IllegalArgumentException("Endereço não pode ser nulo");
        }
        
        // Validar CEP primeiro (antes de verificar se está completo)
        if (endereco.getCep() == null || !endereco.getCep().matches("\\d{8}")) {
            throw new IllegalArgumentException("CEP inválido. Deve conter 8 dígitos.");
        }
        
        // Validar UF antes de verificar se está completo
        if (endereco.getEstado() == null || !endereco.getEstado().matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("Estado inválido. Deve ser sigla de 2 letras maiúsculas (ex: SP).");
        }
        
        // Validar campos obrigatórios
        if (!endereco.isCompleto()) {
            throw new IllegalArgumentException("Endereço incompleto. Todos os campos obrigatórios devem ser preenchidos.");
        }
    }
}

