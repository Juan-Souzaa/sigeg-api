package com.siseg.repository;

import com.siseg.model.Cupom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CupomRepository extends JpaRepository<Cupom, Long> {
    Optional<Cupom> findByCodigo(String codigo);
    
    Optional<Cupom> findByCodigoAndAtivoTrue(String codigo);
    
    @Query("SELECT c FROM Cupom c WHERE c.codigo = :codigo " +
           "AND c.ativo = :ativo " +
           "AND c.dataInicio <= :data " +
           "AND c.dataFim >= :data")
    Optional<Cupom> findByCodigoAndAtivoTrueAndDataValida(@Param("codigo") String codigo, 
                                                           @Param("ativo") Boolean ativo,
                                                           @Param("data") LocalDate data);
    
    Page<Cupom> findByAtivoTrue(Pageable pageable);
}

