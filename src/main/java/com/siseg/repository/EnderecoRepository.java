package com.siseg.repository;

import com.siseg.model.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
    
    List<Endereco> findByClienteId(Long clienteId);
    
    Optional<Endereco> findByClienteIdAndPrincipal(Long clienteId, Boolean principal);
    
    Optional<Endereco> findByIdAndClienteId(Long id, Long clienteId);
    
    List<Endereco> findByRestauranteId(Long restauranteId);
    
    Optional<Endereco> findByRestauranteIdAndPrincipal(Long restauranteId, Boolean principal);
    
    Optional<Endereco> findByIdAndRestauranteId(Long id, Long restauranteId);
    
    long countByClienteId(Long clienteId);
    
    long countByRestauranteId(Long restauranteId);
}

