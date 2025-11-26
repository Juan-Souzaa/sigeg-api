package com.siseg.service;

import com.siseg.dto.entregador.EntregadorRequestDTO;
import com.siseg.dto.entregador.EntregadorResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Entregador;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.DisponibilidadeEntregador;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.mapper.EntregadorMapper;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import com.siseg.util.SecurityUtils;
import com.siseg.validator.EntregadorValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntregadorServiceUnitTest {

    @Mock
    private EntregadorRepository entregadorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private EntregadorMapper entregadorMapper;

    @Mock
    private EntregadorValidator entregadorValidator;

    @InjectMocks
    private EntregadorService entregadorService;

    private EntregadorRequestDTO entregadorRequestDTO;
    private EntregadorResponseDTO entregadorResponseDTO;
    private Entregador entregador;
    private User user;
    private Role roleEntregador;

    @BeforeEach
    void setUp() {
        entregadorRequestDTO = new EntregadorRequestDTO();
        entregadorRequestDTO.setNome("Entregador de Teste");
        entregadorRequestDTO.setEmail("entregador@teste.com");
        entregadorRequestDTO.setCpf("12345678901");
        entregadorRequestDTO.setTelefone("(11) 99999-9999");
        entregadorRequestDTO.setTipoVeiculo(TipoVeiculo.MOTO);
        entregadorRequestDTO.setPlacaVeiculo("ABC1234");
        entregadorRequestDTO.setLatitude(new BigDecimal("-23.5505"));
        entregadorRequestDTO.setLongitude(new BigDecimal("-46.6333"));
        entregadorRequestDTO.setPassword("senha123");

        entregadorResponseDTO = new EntregadorResponseDTO();
        entregadorResponseDTO.setId(1L);
        entregadorResponseDTO.setNome("Entregador de Teste");
        entregadorResponseDTO.setEmail("entregador@teste.com");
        entregadorResponseDTO.setCpf("12345678901");
        entregadorResponseDTO.setStatus(StatusEntregador.PENDING_APPROVAL);
        entregadorResponseDTO.setTipoVeiculo(TipoVeiculo.MOTO);
        entregadorResponseDTO.setUserId(1L);

        user = new User();
        user.setId(1L);
        user.setUsername("entregador@teste.com");

        roleEntregador = new Role();
        roleEntregador.setRoleName(ERole.ROLE_ENTREGADOR);

        entregador = new Entregador();
        entregador.setId(1L);
        entregador.setNome("Entregador de Teste");
        entregador.setEmail("entregador@teste.com");
        entregador.setCpf("12345678901");
        entregador.setStatus(StatusEntregador.PENDING_APPROVAL);
        entregador.setDisponibilidade(DisponibilidadeEntregador.UNAVAILABLE);
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);
        entregador.setUser(user);
    }

    @Test
    void deveCriarEntregadorComSucesso() {
        // Given
        doNothing().when(entregadorValidator).validateEmailUnico(anyString());
        doNothing().when(entregadorValidator).validateCpfUnico(anyString());
        when(roleRepository.findByRoleName(ERole.ROLE_ENTREGADOR)).thenReturn(Optional.of(roleEntregador));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(modelMapper.map(entregadorRequestDTO, Entregador.class)).thenReturn(entregador);
        when(entregadorRepository.save(any(Entregador.class))).thenReturn(entregador);
        when(entregadorMapper.toResponseDTO(any(Entregador.class), anyLong())).thenReturn(entregadorResponseDTO);

        // When
        EntregadorResponseDTO result = entregadorService.criarEntregador(entregadorRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals(entregadorResponseDTO.getId(), result.getId());
        assertEquals(entregadorResponseDTO.getNome(), result.getNome());
        assertEquals(StatusEntregador.PENDING_APPROVAL, result.getStatus());
        verify(entregadorRepository, times(1)).save(any(Entregador.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void deveLancarExcecaoAoCriarEntregadorComEmailExistente() {
        // Given
        doThrow(new IllegalArgumentException("Já existe um usuário com este email."))
                .when(entregadorValidator).validateEmailUnico(anyString());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> entregadorService.criarEntregador(entregadorRequestDTO));
        assertEquals("Já existe um usuário com este email.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoAoCriarEntregadorComCpfExistente() {
        // Given
        doNothing().when(entregadorValidator).validateEmailUnico(anyString());
        doThrow(new IllegalArgumentException("Já existe um entregador com este CPF."))
                .when(entregadorValidator).validateCpfUnico(anyString());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> entregadorService.criarEntregador(entregadorRequestDTO));
        assertEquals("Já existe um entregador com este CPF.", exception.getMessage());
    }

    @Test
    void deveBuscarEntregadorPorIdComSucesso() {
        // Given
        Long id = 1L;
        when(entregadorRepository.findById(id)).thenReturn(Optional.of(entregador));
        when(entregadorMapper.toResponseDTO(any(Entregador.class), anyLong())).thenReturn(entregadorResponseDTO);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            // When
            EntregadorResponseDTO result = entregadorService.buscarPorId(id);

            // Then
            assertNotNull(result);
            assertEquals(id, result.getId());
            verify(entregadorRepository, times(1)).findById(id);
        }
    }

    @Test
    void deveLancarExcecaoAoBuscarEntregadorInexistente() {
        // Given
        Long id = 999L;
        when(entregadorRepository.findById(id)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> entregadorService.buscarPorId(id));
            assertEquals("Entregador não encontrado com ID: " + id, exception.getMessage());
        }
    }

    @Test
    void deveLancarExcecaoAoBuscarEntregadorSemOwnership() {
        // Given
        Long id = 1L;
        User outroUser = new User();
        outroUser.setId(999L);
        
        Entregador outroEntregador = new Entregador();
        outroEntregador.setId(1L);
        outroEntregador.setUser(outroUser);

        when(entregadorRepository.findById(id)).thenReturn(Optional.of(outroEntregador));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);
            mockedSecurityUtils.when(() -> SecurityUtils.validateEntregadorOwnership(any(Entregador.class)))
                    .thenThrow(new AccessDeniedException("Você não tem permissão para acessar este entregador"));

            // When & Then
            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> entregadorService.buscarPorId(id));
            assertEquals("Você não tem permissão para acessar este entregador", exception.getMessage());
        }
    }

    @Test
    void deveListarEntregadoresComPaginacao() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Entregador> page = new PageImpl<>(List.of(entregador), pageable, 1);
        when(entregadorRepository.findAll(pageable)).thenReturn(page);
        when(entregadorMapper.toResponseDTO(any(Entregador.class), anyLong())).thenReturn(entregadorResponseDTO);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            // When
            Page<EntregadorResponseDTO> result = entregadorService.listarTodos(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(entregadorRepository, times(1)).findAll(pageable);
        }
    }

    @Test
    void deveListarEntregadoresPorStatus() {
        // Given
        StatusEntregador status = StatusEntregador.PENDING_APPROVAL;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Entregador> page = new PageImpl<>(List.of(entregador), pageable, 1);
        when(entregadorRepository.findByStatus(status, pageable)).thenReturn(page);
        when(entregadorMapper.toResponseDTO(any(Entregador.class), anyLong())).thenReturn(entregadorResponseDTO);

        // When
        Page<EntregadorResponseDTO> result = entregadorService.findByStatus(status, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(entregadorRepository, times(1)).findByStatus(status, pageable);
    }

    @Test
    void deveAprovarEntregadorComSucesso() {
        Long id = 1L;
        entregador.setStatus(StatusEntregador.APPROVED);
        when(entregadorRepository.findById(id)).thenReturn(Optional.of(entregador));
        when(entregadorRepository.save(any(Entregador.class))).thenReturn(entregador);
        when(entregadorMapper.toResponseDTO(any(Entregador.class), anyLong())).thenReturn(entregadorResponseDTO);

        EntregadorResponseDTO result = entregadorService.aprovarEntregador(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(entregadorRepository, times(1)).findById(id);
        verify(entregadorRepository, times(1)).save(argThat(e -> 
            e.getStatus() == StatusEntregador.APPROVED && 
            e.getDisponibilidade() == DisponibilidadeEntregador.UNAVAILABLE));
    }

    @Test
    void deveRejeitarEntregadorComSucesso() {
        // Given
        Long id = 1L;
        entregador.setStatus(StatusEntregador.REJECTED);
        when(entregadorRepository.findById(id)).thenReturn(Optional.of(entregador));
        when(entregadorRepository.save(any(Entregador.class))).thenReturn(entregador);
        when(entregadorMapper.toResponseDTO(any(Entregador.class), anyLong())).thenReturn(entregadorResponseDTO);

        // When
        EntregadorResponseDTO result = entregadorService.rejeitarEntregador(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(entregadorRepository, times(1)).findById(id);
        verify(entregadorRepository, times(1)).save(argThat(e -> e.getStatus() == StatusEntregador.REJECTED));
    }
}

