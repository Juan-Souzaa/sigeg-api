package com.siseg.repository;

import com.siseg.model.Prato;
import com.siseg.model.enumerations.CategoriaMenu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;



public interface PratoRepository extends JpaRepository<Prato, Long> {

    
    // Métodos com Pageable
    Page<Prato> findByRestauranteIdAndCategoriaAndDisponivel(Long restauranteId, CategoriaMenu categoria, Boolean disponivel, Pageable pageable);
    Page<Prato> findByRestauranteIdAndDisponivel(Long restauranteId, Boolean disponivel, Pageable pageable);
    Page<Prato> findByRestauranteIdAndCategoria(Long restauranteId, CategoriaMenu categoria, Pageable pageable);
    Page<Prato> findByRestauranteId(Long restauranteId, Pageable pageable);
}
