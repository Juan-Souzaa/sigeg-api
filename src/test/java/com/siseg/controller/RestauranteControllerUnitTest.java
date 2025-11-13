package com.siseg.controller;

import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.service.RestauranteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestauranteControllerUnitTest {

    @Mock
    private RestauranteService restauranteService;

    @InjectMocks
    private RestauranteController restauranteController;

    private RestauranteRequestDTO restauranteRequestDTO;
    private RestauranteResponseDTO restauranteResponseDTO;

    @BeforeEach
    void setUp() {
        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro("Rua Teste");
        enderecoDTO.setNumero("123");
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(true);

        restauranteRequestDTO = new RestauranteRequestDTO();
        restauranteRequestDTO.setNome("Restaurante de Teste");
        restauranteRequestDTO.setEmail("teste@restaurante.com");
        restauranteRequestDTO.setTelefone("(11) 99999-9999");
        restauranteRequestDTO.setEndereco(enderecoDTO);

        restauranteResponseDTO = new RestauranteResponseDTO();
        restauranteResponseDTO.setId(1L);
        restauranteResponseDTO.setNome("Restaurante de Teste");
        restauranteResponseDTO.setEmail("teste@restaurante.com");
        restauranteResponseDTO.setTelefone("(11) 99999-9999");
        restauranteResponseDTO.setEndereco("Rua Teste, 123"); // String formatada
        restauranteResponseDTO.setStatus(StatusRestaurante.PENDING_APPROVAL);
    }

    @Test
    void deveCriarRestauranteComSucesso() {
        // Given
        when(restauranteService.criarRestaurante(any(RestauranteRequestDTO.class))).thenReturn(restauranteResponseDTO);

        // When
        ResponseEntity<RestauranteResponseDTO> response = restauranteController.criarRestaurante(restauranteRequestDTO);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(restauranteResponseDTO.getId(), response.getBody().getId());
        assertEquals(restauranteResponseDTO.getNome(), response.getBody().getNome());
        verify(restauranteService, times(1)).criarRestaurante(restauranteRequestDTO);
    }

    @Test
    void deveBuscarRestaurantePorIdComSucesso() {
        // Given
        Long id = 1L;
        when(restauranteService.buscarPorId(id)).thenReturn(restauranteResponseDTO);

        // When
        ResponseEntity<RestauranteResponseDTO> response = restauranteController.buscarPorId(id);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
        verify(restauranteService, times(1)).buscarPorId(id);
    }

    @Test
    void deveRetornarNotFoundQuandoRestauranteNaoExiste() {
        // Given
        Long id = 999L;
        when(restauranteService.buscarPorId(id)).thenThrow(new ResourceNotFoundException("Restaurante não encontrado com ID: " + id));

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> restauranteController.buscarPorId(id));
        verify(restauranteService, times(1)).buscarPorId(id);
    }

    @Test
    void deveListarRestaurantesComPaginacao() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<RestauranteResponseDTO> page = new PageImpl<>(List.of(restauranteResponseDTO), pageable, 1);
        when(restauranteService.listarTodos(pageable)).thenReturn(page);

        // When
        ResponseEntity<Page<RestauranteResponseDTO>> response = restauranteController.listarTodos(pageable);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        verify(restauranteService, times(1)).listarTodos(pageable);
    }

    @Test
    void deveListarRestaurantesPorStatus() {
        // Given
        StatusRestaurante status = StatusRestaurante.PENDING_APPROVAL;
        Pageable pageable = PageRequest.of(0, 10);
        Page<RestauranteResponseDTO> page = new PageImpl<>(List.of(restauranteResponseDTO), pageable, 1);
        when(restauranteService.listarPorStatus(status, pageable)).thenReturn(page);

        // When
        ResponseEntity<Page<RestauranteResponseDTO>> response = restauranteController.listarPorStatus(status, pageable);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        verify(restauranteService, times(1)).listarPorStatus(status, pageable);
    }

    @Test
    void deveAprovarRestauranteComSucesso() {
        // Given
        Long id = 1L;
        restauranteResponseDTO.setStatus(StatusRestaurante.APPROVED);
        when(restauranteService.aprovarRestaurante(id)).thenReturn(restauranteResponseDTO);

        // When
        ResponseEntity<RestauranteResponseDTO> response = restauranteController.aprovarRestaurante(id);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(StatusRestaurante.APPROVED, response.getBody().getStatus());
        verify(restauranteService, times(1)).aprovarRestaurante(id);
    }

    @Test
    void deveRejeitarRestauranteComSucesso() {
        // Given
        Long id = 1L;
        restauranteResponseDTO.setStatus(StatusRestaurante.REJECTED);
        when(restauranteService.rejeitarRestaurante(id)).thenReturn(restauranteResponseDTO);

        // When
        ResponseEntity<RestauranteResponseDTO> response = restauranteController.rejeitarRestaurante(id);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(StatusRestaurante.REJECTED, response.getBody().getStatus());
        verify(restauranteService, times(1)).rejeitarRestaurante(id);
    }
}
