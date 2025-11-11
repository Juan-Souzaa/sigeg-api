package com.siseg.service;

import com.siseg.dto.ticket.TicketComentarioRequestDTO;
import com.siseg.dto.ticket.TicketComentarioResponseDTO;
import com.siseg.dto.ticket.TicketDetalhadoResponseDTO;
import com.siseg.dto.ticket.TicketRequestDTO;
import com.siseg.dto.ticket.TicketResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.mapper.TicketMapper;
import com.siseg.model.Ticket;
import com.siseg.model.TicketComentario;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusTicket;
import com.siseg.model.enumerations.TipoTicket;
import com.siseg.repository.TicketComentarioRepository;
import com.siseg.repository.TicketRepository;
import com.siseg.repository.UserRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.TicketValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
public class TicketService {
    
    private static final Logger logger = Logger.getLogger(TicketService.class.getName());
    
    private final TicketRepository ticketRepository;
    private final TicketComentarioRepository ticketComentarioRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;
    private final TicketValidator ticketValidator;
    private final NotificationService notificationService;
    
    public TicketService(TicketRepository ticketRepository,
                         TicketComentarioRepository ticketComentarioRepository,
                         UserRepository userRepository,
                         TicketMapper ticketMapper,
                         TicketValidator ticketValidator,
                         NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.ticketComentarioRepository = ticketComentarioRepository;
        this.userRepository = userRepository;
        this.ticketMapper = ticketMapper;
        this.ticketValidator = ticketValidator;
        this.notificationService = notificationService;
    }
    
    @Transactional
    public TicketResponseDTO criarTicket(TicketRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        ticketValidator.validatePermissaoCriarTicket(currentUser);
        
        Ticket ticket = criarTicketBasico(dto, currentUser);
        Ticket saved = ticketRepository.save(ticket);
        
        notificarAdminsNovoTicket(saved);
        logger.info("Ticket criado: " + saved.getId() + " por usuario " + currentUser.getId());
        
        return ticketMapper.toResponseDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public TicketResponseDTO buscarPorId(Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        Ticket ticket = buscarTicketValido(id);
        ticketValidator.validatePermissaoVisualizarTicket(ticket, currentUser);
        
        return ticketMapper.toResponseDTO(ticket);
    }
    
    @Transactional(readOnly = true)
    public TicketDetalhadoResponseDTO buscarDetalhadoPorId(Long id) {
        User currentUser = SecurityUtils.getCurrentUser();
        Ticket ticket = buscarTicketValido(id);
        ticketValidator.validatePermissaoVisualizarTicket(ticket, currentUser);
        
        List<TicketComentario> comentarios = buscarComentariosDoTicket(id, currentUser);
        
        return ticketMapper.toDetalhadoResponseDTO(ticket, comentarios);
    }
    
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> listarMeusTickets(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        Page<Ticket> tickets = ticketRepository.findByCriadoPorId(currentUser.getId(), pageable);
        return tickets.map(ticketMapper::toResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> listarTodos(Pageable pageable) {
        SecurityUtils.validateAdminAccess();
        Page<Ticket> tickets = ticketRepository.findAll(pageable);
        return tickets.map(ticketMapper::toResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> listarPorStatus(StatusTicket status, Pageable pageable) {
        SecurityUtils.validateAdminAccess();
        Page<Ticket> tickets = ticketRepository.findByStatus(status, pageable);
        return tickets.map(ticketMapper::toResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<TicketResponseDTO> listarPorTipo(TipoTicket tipo, Pageable pageable) {
        SecurityUtils.validateAdminAccess();
        Page<Ticket> tickets = ticketRepository.findByTipo(tipo, pageable);
        return tickets.map(ticketMapper::toResponseDTO);
    }
    
    @Transactional
    public TicketResponseDTO atribuirTicket(Long ticketId, Long adminId) {
        SecurityUtils.validateAdminAccess();
        Ticket ticket = buscarTicketValido(ticketId);
        User admin = buscarUsuarioValido(adminId);
        validarAdmin(admin);
        
        ticket.setAtribuidoA(admin);
        ticket.setStatus(StatusTicket.EM_ANDAMENTO);
        ticket.setAtualizadoEm(Instant.now());
        
        Ticket saved = ticketRepository.save(ticket);
        notificarAdminAtribuido(saved, admin);
        logger.info("Ticket " + ticketId + " atribuido ao admin " + adminId);
        
        return ticketMapper.toResponseDTO(saved);
    }
    
    @Transactional
    public TicketComentarioResponseDTO adicionarComentario(Long ticketId, TicketComentarioRequestDTO dto) {
        User currentUser = SecurityUtils.getCurrentUser();
        Ticket ticket = buscarTicketValido(ticketId);
        ticketValidator.validatePermissaoComentarTicket(ticket, currentUser);
        
        TicketComentario comentario = criarComentario(ticket, currentUser, dto);
        TicketComentario saved = ticketComentarioRepository.save(comentario);
        
        atualizarTicketAposComentario(ticket);
        notificarComentarioAdicionado(ticket, saved);
        logger.info("Comentario adicionado ao ticket " + ticketId + " por usuario " + currentUser.getId());
        
        return ticketMapper.toComentarioResponseDTO(saved);
    }
    
    @Transactional
    public TicketResponseDTO atualizarStatus(Long ticketId, StatusTicket novoStatus) {
        SecurityUtils.validateAdminAccess();
        Ticket ticket = buscarTicketValido(ticketId);
        
        ticket.setStatus(novoStatus);
        ticket.setAtualizadoEm(Instant.now());
        
        if (novoStatus == StatusTicket.RESOLVIDO || novoStatus == StatusTicket.FECHADO) {
            ticket.setResolvidoEm(Instant.now());
        }
        
        Ticket saved = ticketRepository.save(ticket);
        notificarMudancaStatus(saved);
        logger.info("Status do ticket " + ticketId + " atualizado para " + novoStatus);
        
        return ticketMapper.toResponseDTO(saved);
    }
    
    @Transactional
    public TicketResponseDTO resolverTicket(Long ticketId, String resolucao) {
        SecurityUtils.validateAdminAccess();
        Ticket ticket = buscarTicketValido(ticketId);
        ticketValidator.validateTicketAberto(ticket);
        
        ticket.setStatus(StatusTicket.RESOLVIDO);
        ticket.setResolvidoEm(Instant.now());
        ticket.setAtualizadoEm(Instant.now());
        
        Ticket saved = ticketRepository.save(ticket);
        
        adicionarComentarioResolucao(saved, resolucao);
        notificarMudancaStatus(saved);
        logger.info("Ticket " + ticketId + " resolvido");
        
        return ticketMapper.toResponseDTO(saved);
    }
    
    private Ticket criarTicketBasico(TicketRequestDTO dto, User currentUser) {
        Ticket ticket = new Ticket();
        ticket.setTitulo(dto.getTitulo());
        ticket.setDescricao(dto.getDescricao());
        ticket.setTipo(dto.getTipo());
        ticket.setPrioridade(dto.getPrioridade());
        ticket.setStatus(StatusTicket.ABERTO);
        ticket.setCriadoPor(currentUser);
        return ticket;
    }
    
    private Ticket buscarTicketValido(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket nao encontrado com ID: " + id));
    }
    
    private User buscarUsuarioValido(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com ID: " + id));
    }
    
    private void validarAdmin(User user) {
        boolean eAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName() == ERole.ROLE_ADMIN);
        
        if (!eAdmin) {
            throw new IllegalArgumentException("Usuario nao e administrador");
        }
    }
    
    private List<TicketComentario> buscarComentariosDoTicket(Long ticketId, User currentUser) {
        List<TicketComentario> todosComentarios = ticketComentarioRepository.findByTicketIdOrderByCriadoEmAsc(ticketId);
        
        boolean eAdmin = SecurityUtils.isAdmin();
        
        return todosComentarios.stream()
                .filter(comentario -> eAdmin || !comentario.getInterno())
                .collect(Collectors.toList());
    }
    
    private TicketComentario criarComentario(Ticket ticket, User autor, TicketComentarioRequestDTO dto) {
        TicketComentario comentario = new TicketComentario();
        comentario.setTicket(ticket);
        comentario.setAutor(autor);
        comentario.setComentario(dto.getComentario());
        comentario.setInterno(dto.getInterno() != null && dto.getInterno() && SecurityUtils.isAdmin());
        return comentario;
    }
    
    private void atualizarTicketAposComentario(Ticket ticket) {
        if (ticket.getStatus() == StatusTicket.ABERTO) {
            ticket.setStatus(StatusTicket.EM_ANDAMENTO);
        }
        ticket.setAtualizadoEm(Instant.now());
        ticketRepository.save(ticket);
    }
    
    private void adicionarComentarioResolucao(Ticket ticket, String resolucao) {
        User admin = SecurityUtils.getCurrentUser();
        TicketComentarioRequestDTO dto = new TicketComentarioRequestDTO();
        dto.setComentario("Resolucao: " + resolucao);
        dto.setInterno(false);
        
        TicketComentario comentario = criarComentario(ticket, admin, dto);
        ticketComentarioRepository.save(comentario);
    }
    
    private void notificarAdminsNovoTicket(Ticket ticket) {
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getRoleName() == ERole.ROLE_ADMIN))
                .collect(Collectors.toList());
        
        String assunto = "Novo ticket criado: " + ticket.getTitulo();
        String mensagem = String.format("Um novo ticket #%d foi criado.\nTipo: %s\nPrioridade: %s\n\n%s",
                ticket.getId(), ticket.getTipo(), ticket.getPrioridade(), ticket.getDescricao());
        
        for (User admin : admins) {
            if (admin.getUsername() != null) {
                notificationService.sendEmail(admin.getUsername(), assunto, mensagem);
            }
        }
    }
    
    private void notificarAdminAtribuido(Ticket ticket, User admin) {
        String assunto = "Ticket atribuido a voce: " + ticket.getTitulo();
        String mensagem = String.format("O ticket #%d foi atribuido a voce.\nTipo: %s\nPrioridade: %s",
                ticket.getId(), ticket.getTipo(), ticket.getPrioridade());
        
        if (admin.getUsername() != null) {
            notificationService.sendEmail(admin.getUsername(), assunto, mensagem);
        }
    }
    
    private void notificarComentarioAdicionado(Ticket ticket, TicketComentario comentario) {
        String assunto = "Novo comentario no ticket #" + ticket.getId();
        String mensagem = String.format("Um novo comentario foi adicionado ao ticket #%d:\n\n%s",
                ticket.getId(), comentario.getComentario());
        
        if (ticket.getCriadoPor().getUsername() != null) {
            notificationService.sendEmail(ticket.getCriadoPor().getUsername(), assunto, mensagem);
        }
        
        if (ticket.getAtribuidoA() != null && ticket.getAtribuidoA().getUsername() != null) {
            notificationService.sendEmail(ticket.getAtribuidoA().getUsername(), assunto, mensagem);
        }
    }
    
    private void notificarMudancaStatus(Ticket ticket) {
        String assunto = "Status do ticket #" + ticket.getId() + " atualizado";
        String mensagem = String.format("O status do ticket #%d foi atualizado para: %s",
                ticket.getId(), ticket.getStatus());
        
        if (ticket.getCriadoPor().getUsername() != null) {
            notificationService.sendEmail(ticket.getCriadoPor().getUsername(), assunto, mensagem);
        }
    }
}

