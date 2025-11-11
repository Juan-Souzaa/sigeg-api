package com.siseg.repository;

import com.siseg.model.Ticket;
import com.siseg.model.enumerations.PrioridadeTicket;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.model.enumerations.TipoTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByCriadoPorId(Long userId, Pageable pageable);
    
    Page<Ticket> findByAtribuidoAId(Long userId, Pageable pageable);
    
    Page<Ticket> findByStatus(StatusTicket status, Pageable pageable);
    
    Page<Ticket> findByTipo(TipoTicket tipo, Pageable pageable);
    
    Page<Ticket> findByPrioridade(PrioridadeTicket prioridade, Pageable pageable);
}

