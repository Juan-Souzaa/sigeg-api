package com.siseg.repository;

import com.siseg.model.TicketComentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketComentarioRepository extends JpaRepository<TicketComentario, Long> {
    List<TicketComentario> findByTicketIdOrderByCriadoEmAsc(Long ticketId);
}

