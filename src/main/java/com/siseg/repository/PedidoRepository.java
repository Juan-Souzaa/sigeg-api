package com.siseg.repository;

import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteId(Long clienteId);
    List<Pedido> findByRestauranteId(Long restauranteId);
    List<Pedido> findByStatus(StatusPedido status);
}
