package com.siseg.repository;

import com.siseg.model.Entregador;
import com.siseg.model.enumerations.StatusEntregador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntregadorRepository extends JpaRepository<Entregador, Long> {
    Optional<Entregador> findByUserId(Long userId);
    Page<Entregador> findByStatus(StatusEntregador status, Pageable pageable);
    Optional<Entregador> findByEmail(String email);
    Optional<Entregador> findByCpf(String cpf);
}

