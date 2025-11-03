package com.siseg.repository;

import com.siseg.model.Avaliacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    
    Optional<Avaliacao> findByPedidoId(Long pedidoId);
    
    Page<Avaliacao> findByRestauranteId(Long restauranteId, Pageable pageable);
    
    Page<Avaliacao> findByRestauranteIdOrderByCriadoEmDesc(Long restauranteId, Pageable pageable);
    
    Page<Avaliacao> findByEntregadorId(Long entregadorId, Pageable pageable);
    
    Optional<Avaliacao> findByClienteIdAndPedidoId(Long clienteId, Long pedidoId);
    
    boolean existsByClienteIdAndPedidoId(Long clienteId, Long pedidoId);
    
    long countByRestauranteId(Long restauranteId);
    
    long countByEntregadorId(Long entregadorId);
    
    @Query("SELECT AVG(a.notaRestaurante) FROM Avaliacao a WHERE a.restaurante.id = :restauranteId")
    BigDecimal calcularMediaNotaRestaurante(@Param("restauranteId") Long restauranteId);
    
    @Query("SELECT AVG(a.notaEntregador) FROM Avaliacao a WHERE a.entregador.id = :entregadorId AND a.notaEntregador IS NOT NULL")
    BigDecimal calcularMediaNotaEntregador(@Param("entregadorId") Long entregadorId);
}

