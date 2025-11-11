package com.siseg.mapper;

import com.siseg.dto.ticket.TicketComentarioResponseDTO;
import com.siseg.dto.ticket.TicketDetalhadoResponseDTO;
import com.siseg.dto.ticket.TicketResponseDTO;
import com.siseg.model.Ticket;
import com.siseg.model.TicketComentario;
import com.siseg.model.User;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TicketMapper {
    
    private final ModelMapper modelMapper;
    
    public TicketMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }
    
    public TicketResponseDTO toResponseDTO(Ticket ticket) {
        TicketResponseDTO dto = modelMapper.map(ticket, TicketResponseDTO.class);
        dto.setCriadoPorId(ticket.getCriadoPor().getId());
        dto.setCriadoPorNome(obterNomeUsuario(ticket.getCriadoPor()));
        
        if (ticket.getAtribuidoA() != null) {
            dto.setAtribuidoAId(ticket.getAtribuidoA().getId());
            dto.setAtribuidoANome(obterNomeUsuario(ticket.getAtribuidoA()));
        }
        
        return dto;
    }
    
    public TicketDetalhadoResponseDTO toDetalhadoResponseDTO(Ticket ticket, List<TicketComentario> comentarios) {
        TicketDetalhadoResponseDTO dto = new TicketDetalhadoResponseDTO();
        dto.setTicket(toResponseDTO(ticket));
        
        List<TicketComentarioResponseDTO> comentariosDTO = comentarios.stream()
                .map(this::toComentarioResponseDTO)
                .collect(Collectors.toList());
        
        dto.setComentarios(comentariosDTO);
        return dto;
    }
    
    public TicketComentarioResponseDTO toComentarioResponseDTO(TicketComentario comentario) {
        TicketComentarioResponseDTO dto = modelMapper.map(comentario, TicketComentarioResponseDTO.class);
        dto.setTicketId(comentario.getTicket().getId());
        dto.setAutorId(comentario.getAutor().getId());
        dto.setAutorNome(obterNomeUsuario(comentario.getAutor()));
        return dto;
    }
    
    private String obterNomeUsuario(User user) {
        if (user == null) {
            return null;
        }
        
        return user.getUsername();
    }
}

