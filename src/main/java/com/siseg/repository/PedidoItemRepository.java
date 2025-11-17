package com.siseg.repository;

import com.siseg.model.PedidoItem;
import com.siseg.model.enumerations.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {
    
    @Query("SELECT pi FROM PedidoItem pi JOIN pi.pedido p WHERE pi.prato.id = :pratoId AND p.status IN :statuses")
    List<PedidoItem> findByPratoIdAndPedidoStatusIn(@Param("pratoId") Long pratoId, @Param("statuses") List<StatusPedido> statuses);
}

