package com.siseg.repository;

import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.StatusRestaurante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    
    Page<Restaurante> findByStatus(StatusRestaurante status, Pageable pageable);
    
    Optional<Restaurante> findByUserId(Long userId);
    
    Optional<Restaurante> findByEmail(String email);
    
    @Query("SELECT r FROM Restaurante r WHERE r.status = 'APPROVED' " +
           "AND (:cozinha IS NULL OR LOWER(r.nome) LIKE LOWER(CONCAT('%', :cozinha, '%')))")
    Page<Restaurante> buscarRestaurantesAprovados(@Param("cozinha") String cozinha, Pageable pageable);
}