package com.siseg.service;

import com.siseg.dto.cardapio.CardapioResponseDTO;
import com.siseg.dto.prato.PratoRequestDTO;
import com.siseg.dto.prato.PratoResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.mapper.PedidoMapper;
import com.siseg.model.Prato;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.repository.PratoRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PratoServiceUnitTest {

    @Mock
    private PratoRepository pratoRepository;

    @Mock
    private RestauranteRepository restauranteRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PedidoMapper pedidoMapper;

    @InjectMocks
    private PratoService pratoService;

    private User user;
    private Restaurante restaurante;
    private Prato prato;
    private PratoRequestDTO pratoRequestDTO;
    private PratoResponseDTO pratoResponseDTO;

    @BeforeEach
    void setUp() {
        try {
            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("test-uploads");
            ReflectionTestUtils.setField(pratoService, "uploadDir", tempDir.toString());
        } catch (Exception e) {
            ReflectionTestUtils.setField(pratoService, "uploadDir", System.getProperty("java.io.tmpdir"));
        }
        user = new User();
        user.setId(1L);
        user.setUsername("restaurante@teste.com");

        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setNome("Restaurante Teste");
        restaurante.setUser(user);

        prato = new Prato();
        prato.setId(1L);
        prato.setNome("Prato Teste");
        prato.setDescricao("Descrição do prato");
        prato.setPreco(new BigDecimal("25.50"));
        prato.setCategoria(CategoriaMenu.MAIN);
        prato.setDisponivel(true);
        prato.setRestaurante(restaurante);

        pratoRequestDTO = new PratoRequestDTO();
        pratoRequestDTO.setNome("Prato Teste");
        pratoRequestDTO.setDescricao("Descrição do prato");
        pratoRequestDTO.setPreco(new BigDecimal("25.50"));
        pratoRequestDTO.setCategoria(CategoriaMenu.MAIN);
        pratoRequestDTO.setDisponivel(true);

        pratoResponseDTO = new PratoResponseDTO();
        pratoResponseDTO.setId(1L);
        pratoResponseDTO.setNome("Prato Teste");
        pratoResponseDTO.setPreco(new BigDecimal("25.50"));
    }

    @Test
    void deveCriarPratoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(modelMapper.map(pratoRequestDTO, Prato.class)).thenReturn(prato);
            when(pratoRepository.save(any(Prato.class))).thenAnswer(invocation -> {
                Prato p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(modelMapper.map(any(Prato.class), eq(PratoResponseDTO.class))).thenReturn(pratoResponseDTO);

            PratoResponseDTO result = pratoService.criarPrato(1L, pratoRequestDTO);

            assertNotNull(result);
            verify(pratoRepository, times(1)).save(any(Prato.class));
            verify(restauranteRepository, times(1)).findById(1L);
        }
    }

    @Test
    void deveLancarExcecaoQuandoRestauranteNaoEncontrado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(restauranteRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> pratoService.criarPrato(1L, pratoRequestDTO));
        }
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaCriarPrato() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            Restaurante outroRestaurante = new Restaurante();
            outroRestaurante.setId(2L);
            outroRestaurante.setUser(new User());

            when(restauranteRepository.findById(2L)).thenReturn(Optional.of(outroRestaurante));
            doThrow(new AccessDeniedException("Você não tem permissão para acessar este restaurante"))
                    .when(SecurityUtils.class);
            SecurityUtils.validateRestauranteOwnership(outroRestaurante);

            assertThrows(AccessDeniedException.class, 
                    () -> pratoService.criarPrato(2L, pratoRequestDTO));
        }
    }

    @Test
    void deveCriarPratoComFoto() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            MultipartFile foto = mock(MultipartFile.class);
            when(foto.isEmpty()).thenReturn(false);
            when(foto.getOriginalFilename()).thenReturn("foto.jpg");
            try {
                when(foto.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("test".getBytes()));
            } catch (Exception e) {
                // Ignore
            }
            pratoRequestDTO.setFoto(foto);

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(modelMapper.map(pratoRequestDTO, Prato.class)).thenReturn(prato);
            when(pratoRepository.save(any(Prato.class))).thenAnswer(invocation -> {
                Prato p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(modelMapper.map(any(Prato.class), eq(PratoResponseDTO.class))).thenReturn(pratoResponseDTO);

            PratoResponseDTO result = pratoService.criarPrato(1L, pratoRequestDTO);

            assertNotNull(result);
            verify(pratoRepository, times(1)).save(any(Prato.class));
        }
    }

    @Test
    void deveAtualizarPratoComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            PratoRequestDTO updateDTO = new PratoRequestDTO();
            updateDTO.setNome("Prato Atualizado");
            updateDTO.setPreco(new BigDecimal("30.00"));

            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pratoRepository.save(any(Prato.class))).thenReturn(prato);
            doAnswer(invocation -> {
                PratoRequestDTO dto = invocation.getArgument(0);
                Prato p = invocation.getArgument(1);
                if (dto.getNome() != null) p.setNome(dto.getNome());
                if (dto.getPreco() != null) p.setPreco(dto.getPreco());
                return null;
            }).when(modelMapper).map(any(PratoRequestDTO.class), any(Prato.class));
            when(modelMapper.map(any(Prato.class), eq(PratoResponseDTO.class))).thenReturn(pratoResponseDTO);

            PratoResponseDTO result = pratoService.atualizarPrato(1L, updateDTO);

            assertNotNull(result);
            verify(pratoRepository, times(1)).save(prato);
        }
    }

    @Test
    void deveLancarExcecaoQuandoPratoNaoEncontrado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(pratoRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> pratoService.atualizarPrato(1L, pratoRequestDTO));
        }
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaAtualizarPrato() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            Restaurante outroRestaurante = new Restaurante();
            outroRestaurante.setUser(new User());
            prato.setRestaurante(outroRestaurante);

            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            doThrow(new AccessDeniedException("Você não tem permissão para acessar este restaurante"))
                    .when(SecurityUtils.class);
            SecurityUtils.validateRestauranteOwnership(outroRestaurante);

            assertThrows(AccessDeniedException.class, 
                    () -> pratoService.atualizarPrato(1L, pratoRequestDTO));
        }
    }

    @Test
    void deveAlternarDisponibilidadeComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            prato.setDisponivel(true);

            when(pratoRepository.findById(1L)).thenReturn(Optional.of(prato));
            when(pratoRepository.save(any(Prato.class))).thenReturn(prato);
            when(modelMapper.map(any(Prato.class), eq(PratoResponseDTO.class))).thenReturn(pratoResponseDTO);

            PratoResponseDTO result = pratoService.alternarDisponibilidade(1L);

            assertNotNull(result);
            assertFalse(prato.getDisponivel());
            verify(pratoRepository, times(1)).save(prato);
        }
    }

    @Test
    void deveListarPorRestauranteComFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Prato> pratosPage = new PageImpl<>(List.of(prato), pageable, 1);

        when(pratoRepository.findByRestauranteIdAndCategoriaAndDisponivel(1L, CategoriaMenu.MAIN, true, pageable))
                .thenReturn(pratosPage);
        when(modelMapper.map(any(Prato.class), eq(PratoResponseDTO.class))).thenReturn(pratoResponseDTO);

        Page<PratoResponseDTO> result = pratoService.listarPorRestaurante(1L, CategoriaMenu.MAIN, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(pratoRepository, times(1)).findByRestauranteIdAndCategoriaAndDisponivel(1L, CategoriaMenu.MAIN, true, pageable);
    }

    @Test
    void deveListarPorRestauranteSemFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Prato> pratosPage = new PageImpl<>(List.of(prato), pageable, 1);

        when(pratoRepository.findByRestauranteId(1L, pageable)).thenReturn(pratosPage);
        when(modelMapper.map(any(Prato.class), eq(PratoResponseDTO.class))).thenReturn(pratoResponseDTO);

        Page<PratoResponseDTO> result = pratoService.listarPorRestaurante(1L, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(pratoRepository, times(1)).findByRestauranteId(1L, pageable);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveBuscarCardapioComSucesso() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Prato> pratosPage = new PageImpl<>(List.of(prato), pageable, 1);
        CardapioResponseDTO cardapioDTO = new CardapioResponseDTO();
        cardapioDTO.setRestauranteId(1L);
        cardapioDTO.setRestauranteNome("Restaurante Teste");

        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
        when(pratoRepository.findByRestauranteId(1L, pageable)).thenReturn(pratosPage);
        when(pedidoMapper.toCardapioResponseDTO(eq(1L), eq("Restaurante Teste"), any(Map.class)))
                .thenReturn(cardapioDTO);

        CardapioResponseDTO result = pratoService.buscarCardapio(1L, pageable);

        assertNotNull(result);
        assertEquals(1L, result.getRestauranteId());
        verify(pratoRepository, times(1)).findByRestauranteId(1L, pageable);
    }
}

