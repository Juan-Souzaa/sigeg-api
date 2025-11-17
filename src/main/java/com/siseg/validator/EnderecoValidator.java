package com.siseg.validator;

import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Restaurante;
import com.siseg.util.SecurityUtils;
import org.springframework.stereotype.Component;

@Component
public class EnderecoValidator {
    
    public void validate(Endereco endereco) {
        if (endereco == null) {
            throw new IllegalArgumentException("Endereço não pode ser nulo");
        }
        
        if (endereco.getCep() == null || !endereco.getCep().matches("\\d{8}")) {
            throw new IllegalArgumentException("CEP inválido. Deve conter 8 dígitos.");
        }
        
        if (endereco.getEstado() == null || !endereco.getEstado().matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("Estado inválido. Deve ser sigla de 2 letras maiúsculas (ex: SP).");
        }
        
        if (!endereco.isCompleto()) {
            throw new IllegalArgumentException("Endereço incompleto. Todos os campos obrigatórios devem ser preenchidos.");
        }
    }
    
    public void validateEnderecoPertenceAoCliente(Endereco endereco, Long clienteId, Cliente cliente) {
        if (endereco == null) {
            throw new ResourceNotFoundException("Endereço não encontrado");
        }
        
        if (endereco.getCliente() == null || !endereco.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Endereço não pertence ao cliente");
        }
        
        if (cliente == null) {
            throw new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId);
        }
        
        SecurityUtils.validateClienteOwnership(cliente);
    }
    
    public void validateEnderecoPertenceAoRestaurante(Endereco endereco, Long restauranteId, Restaurante restaurante) {
        if (endereco == null) {
            throw new ResourceNotFoundException("Endereço não encontrado");
        }
        
        if (endereco.getRestaurante() == null || !endereco.getRestaurante().getId().equals(restauranteId)) {
            throw new IllegalArgumentException("Endereço não pertence ao restaurante");
        }
        
        if (restaurante == null) {
            throw new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId);
        }
        
        SecurityUtils.validateRestauranteOwnership(restaurante);
    }
    
    public void validatePodeExcluirEndereco(long totalEnderecos, String tipo) {
        if (totalEnderecos <= 1) {
            throw new IllegalArgumentException("Não é possível excluir o único endereço do " + tipo);
        }
    }
    
    public void validateClienteOwnership(Cliente cliente) {
        if (cliente == null) {
            throw new ResourceNotFoundException("Cliente não encontrado");
        }
        SecurityUtils.validateClienteOwnership(cliente);
    }
    
    public void validateRestauranteOwnership(Restaurante restaurante) {
        if (restaurante == null) {
            throw new ResourceNotFoundException("Restaurante não encontrado");
        }
        SecurityUtils.validateRestauranteOwnership(restaurante);
    }
}

