package com.siseg.repository;

import com.siseg.model.ConfiguracaoTaxa;
import com.siseg.model.enumerations.TipoTaxa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracaoTaxaRepository extends JpaRepository<ConfiguracaoTaxa, Long> {
    Optional<ConfiguracaoTaxa> findByTipoTaxaAndAtivoTrue(TipoTaxa tipoTaxa);
    List<ConfiguracaoTaxa> findByTipoTaxaOrderByCriadoEmDesc(TipoTaxa tipoTaxa);
}

