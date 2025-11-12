package com.siseg.repository;

import com.siseg.model.RotaEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RotaEntregaRepository extends JpaRepository<RotaEntrega, Long> {
    
    Optional<RotaEntrega> findByPedidoId(Long pedidoId);
}

