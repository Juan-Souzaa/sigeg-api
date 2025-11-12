package com.siseg.repository;

import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteId(Long clienteId);
    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);
    Page<Pedido> findByClienteIdAndStatus(Long clienteId, StatusPedido status, Pageable pageable);
    Page<Pedido> findByClienteIdAndCriadoEmBetween(Long clienteId, Instant dataInicio, Instant dataFim, Pageable pageable);
    Page<Pedido> findByClienteIdAndRestauranteId(Long clienteId, Long restauranteId, Pageable pageable);
    Page<Pedido> findByClienteIdAndStatusAndCriadoEmBetween(Long clienteId, StatusPedido status, Instant dataInicio, Instant dataFim, Pageable pageable);
    List<Pedido> findByRestauranteId(Long restauranteId);
    Page<Pedido> findByRestauranteId(Long restauranteId, Pageable pageable);
    Page<Pedido> findByRestauranteIdAndStatus(Long restauranteId, StatusPedido status, Pageable pageable);
    Page<Pedido> findByRestauranteIdAndCriadoEmBetween(Long restauranteId, Instant dataInicio, Instant dataFim, Pageable pageable);
    Page<Pedido> findByRestauranteIdAndStatusAndCriadoEmBetween(Long restauranteId, StatusPedido status, Instant dataInicio, Instant dataFim, Pageable pageable);
    List<Pedido> findByStatus(StatusPedido status);
    List<Pedido> findByEntregadorId(Long entregadorId);
    Page<Pedido> findByStatusAndEntregadorIsNull(StatusPedido status, Pageable pageable);
    List<Pedido> findByStatusAndRestauranteIdAndCriadoEmBetween(StatusPedido status, Long restauranteId, Instant inicio, Instant fim);
    List<Pedido> findByStatusAndEntregadorIdAndCriadoEmBetween(StatusPedido status, Long entregadorId, Instant inicio, Instant fim);
}
