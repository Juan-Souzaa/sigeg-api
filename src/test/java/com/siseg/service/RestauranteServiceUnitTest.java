package com.siseg.service;

import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.RestauranteRepository;
import com.siseg.util.SecurityUtils;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestauranteServiceUnitTest {

    @Mock
    private RestauranteRepository restauranteRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private RestauranteService restauranteService;

    private RestauranteRequestDTO restauranteRequestDTO;
    private RestauranteResponseDTO restauranteResponseDTO;
    private Restaurante restaurante;

    @BeforeEach
    void setUp() {
        restauranteRequestDTO = new RestauranteRequestDTO();
        restauranteRequestDTO.setNome("Restaurante de Teste");
        restauranteRequestDTO.setEmail("teste@restaurante.com");
        restauranteRequestDTO.setTelefone("(11) 99999-9999");
        restauranteRequestDTO.setEndereco("Rua Teste, 123");

        restauranteResponseDTO = new RestauranteResponseDTO();
        restauranteResponseDTO.setId(1L);
        restauranteResponseDTO.setNome("Restaurante de Teste");
        restauranteResponseDTO.setEmail("teste@restaurante.com");
        restauranteResponseDTO.setTelefone("(11) 99999-9999");
        restauranteResponseDTO.setEndereco("Rua Teste, 123");
        restauranteResponseDTO.setStatus(StatusRestaurante.PENDING_APPROVAL);

        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setNome("Restaurante de Teste");
        restaurante.setEmail("teste@restaurante.com");
        restaurante.setTelefone("(11) 99999-9999");
        restaurante.setEndereco("Rua Teste, 123");
        restaurante.setStatus(StatusRestaurante.PENDING_APPROVAL);
    }

    @Test
    void deveCriarRestauranteComSucesso() {
        // Given
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        
        when(modelMapper.map(restauranteRequestDTO, Restaurante.class)).thenReturn(restaurante);
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(modelMapper.map(restaurante, RestauranteResponseDTO.class)).thenReturn(restauranteResponseDTO);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(mockUser);

            // When
            RestauranteResponseDTO result = restauranteService.criarRestaurante(restauranteRequestDTO);

            // Then
            assertNotNull(result);
            assertEquals(restauranteResponseDTO.getId(), result.getId());
            assertEquals(restauranteResponseDTO.getNome(), result.getNome());
            assertEquals(StatusRestaurante.PENDING_APPROVAL, result.getStatus());
            verify(restauranteRepository, times(1)).save(argThat(r -> r.getUser() != null && r.getUser().getId().equals(mockUser.getId())));
        }
    }

    @Test
    void deveBuscarRestaurantePorIdComSucesso() {
        // Given
        Long id = 1L;
        when(restauranteRepository.findById(id)).thenReturn(Optional.of(restaurante));
        when(modelMapper.map(restaurante, RestauranteResponseDTO.class)).thenReturn(restauranteResponseDTO);

        // When
        RestauranteResponseDTO result = restauranteService.buscarPorId(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(restauranteRepository, times(1)).findById(id);
    }

    @Test
    void deveLancarExcecaoAoBuscarRestauranteInexistente() {
        // Given
        Long id = 999L;
        when(restauranteRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> restauranteService.buscarPorId(id));
        assertEquals("Restaurante não encontrado com ID: " + id, exception.getMessage());
    }

    @Test
    void deveListarRestaurantesComPaginacao() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Restaurante> page = new PageImpl<>(List.of(restaurante), pageable, 1);
        when(restauranteRepository.findAll(pageable)).thenReturn(page);
        when(modelMapper.map(restaurante, RestauranteResponseDTO.class)).thenReturn(restauranteResponseDTO);

        // When
        Page<RestauranteResponseDTO> result = restauranteService.listarTodos(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(restauranteRepository, times(1)).findAll(pageable);
    }

    @Test
    void deveListarRestaurantesPorStatus() {
        // Given
        StatusRestaurante status = StatusRestaurante.PENDING_APPROVAL;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Restaurante> page = new PageImpl<>(List.of(restaurante), pageable, 1);
        when(restauranteRepository.findByStatus(status, pageable)).thenReturn(page);
        when(modelMapper.map(restaurante, RestauranteResponseDTO.class)).thenReturn(restauranteResponseDTO);

        // When
        Page<RestauranteResponseDTO> result = restauranteService.listarPorStatus(status, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(restauranteRepository, times(1)).findByStatus(status, pageable);
    }

    @Test
    void deveAprovarRestauranteComSucesso() {
        // Given
        Long id = 1L;
        restaurante.setStatus(StatusRestaurante.APPROVED);
        when(restauranteRepository.findById(id)).thenReturn(Optional.of(restaurante));
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(modelMapper.map(restaurante, RestauranteResponseDTO.class)).thenReturn(restauranteResponseDTO);

        // When
        RestauranteResponseDTO result = restauranteService.aprovarRestaurante(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(restauranteRepository, times(1)).findById(id);
        verify(restauranteRepository, times(1)).save(any(Restaurante.class));
    }

    @Test
    void deveRejeitarRestauranteComSucesso() {
        // Given
        Long id = 1L;
        restaurante.setStatus(StatusRestaurante.REJECTED);
        when(restauranteRepository.findById(id)).thenReturn(Optional.of(restaurante));
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(modelMapper.map(restaurante, RestauranteResponseDTO.class)).thenReturn(restauranteResponseDTO);

        // When
        RestauranteResponseDTO result = restauranteService.rejeitarRestaurante(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(restauranteRepository, times(1)).findById(id);
        verify(restauranteRepository, times(1)).save(any(Restaurante.class));
    }
}
