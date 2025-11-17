package com.siseg.service;

import com.siseg.dto.cupom.CupomRequestDTO;
import com.siseg.dto.cupom.CupomResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cupom;
import com.siseg.model.enumerations.TipoDesconto;
import com.siseg.mapper.CupomMapper;
import com.siseg.repository.CupomRepository;
import com.siseg.validator.CupomValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CupomServiceUnitTest {

    @Mock
    private CupomRepository cupomRepository;

    @Mock
    private CupomValidator cupomValidator;

    @Mock
    private CupomMapper cupomMapper;

    @InjectMocks
    private CupomService cupomService;

    private CupomRequestDTO requestDTO;
    private Cupom cupom;
    private CupomResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new CupomRequestDTO();
        requestDTO.setCodigo("DESCONTO10");
        requestDTO.setTipoDesconto(TipoDesconto.PERCENTUAL);
        requestDTO.setValorDesconto(new BigDecimal("10.00"));
        requestDTO.setValorMinimo(new BigDecimal("50.00"));
        requestDTO.setDataInicio(LocalDate.now());
        requestDTO.setDataFim(LocalDate.now().plusDays(30));
        requestDTO.setUsosMaximos(100);

        cupom = new Cupom();
        cupom.setId(1L);
        cupom.setCodigo("DESCONTO10");
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValorDesconto(new BigDecimal("10.00"));
        cupom.setValorMinimo(new BigDecimal("50.00"));
        cupom.setDataInicio(LocalDate.now());
        cupom.setDataFim(LocalDate.now().plusDays(30));
        cupom.setUsosMaximos(100);
        cupom.setUsosAtuais(0);
        cupom.setAtivo(true);

        responseDTO = new CupomResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setCodigo("DESCONTO10");
        responseDTO.setTipoDesconto(TipoDesconto.PERCENTUAL);
        responseDTO.setValorDesconto(new BigDecimal("10.00"));
        responseDTO.setAtivo(true);
    }

    @Test
    void deveCriarCupomComSucesso() {
        doNothing().when(cupomValidator).validateCodigoUnico("DESCONTO10");
        when(cupomRepository.save(any(Cupom.class))).thenReturn(cupom);
        when(cupomMapper.toResponseDTO(cupom)).thenReturn(responseDTO);

        CupomResponseDTO resultado = cupomService.criarCupom(requestDTO);

        assertNotNull(resultado);
        assertEquals("DESCONTO10", resultado.getCodigo());
        verify(cupomValidator).validateCodigoUnico("DESCONTO10");
        verify(cupomRepository).save(any(Cupom.class));
    }

    @Test
    void deveLancarExcecaoQuandoDataInicioDepoisDeDataFim() {
        requestDTO.setDataInicio(LocalDate.now().plusDays(10));
        requestDTO.setDataFim(LocalDate.now());

        doNothing().when(cupomValidator).validateCodigoUnico("DESCONTO10");

        assertThrows(IllegalArgumentException.class, () -> cupomService.criarCupom(requestDTO));
    }

    @Test
    void deveBuscarCupomPorCodigoComSucesso() {
        LocalDate hoje = LocalDate.now();
        when(cupomRepository.findByCodigoAndAtivoTrueAndDataValida("DESCONTO10", true, hoje))
                .thenReturn(Optional.of(cupom));

        Cupom resultado = cupomService.buscarPorCodigo("DESCONTO10");

        assertNotNull(resultado);
        assertEquals("DESCONTO10", resultado.getCodigo());
    }

    @Test
    void deveLancarExcecaoQuandoCupomNaoEncontrado() {
        LocalDate hoje = LocalDate.now();
        when(cupomRepository.findByCodigoAndAtivoTrueAndDataValida("INVALIDO", true, hoje))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cupomService.buscarPorCodigo("INVALIDO"));
    }

    @Test
    void deveListarCuponsAtivosComSucesso() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Cupom> cuponsPage = new PageImpl<>(List.of(cupom));
        when(cupomRepository.findByAtivoTrue(pageable)).thenReturn(cuponsPage);
        when(cupomMapper.toResponseDTO(cupom)).thenReturn(responseDTO);

        Page<CupomResponseDTO> resultado = cupomService.listarCuponsAtivos(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
    }

    @Test
    void deveDesativarCupomComSucesso() {
        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));
        when(cupomRepository.save(cupom)).thenReturn(cupom);
        when(cupomMapper.toResponseDTO(cupom)).thenReturn(responseDTO);

        CupomResponseDTO resultado = cupomService.desativarCupom(1L);

        assertFalse(cupom.getAtivo());
        assertNotNull(resultado);
        verify(cupomRepository).save(cupom);
    }

    @Test
    void deveIncrementarUsoCupom() {
        int usosIniciais = cupom.getUsosAtuais();
        when(cupomRepository.save(cupom)).thenReturn(cupom);

        cupomService.incrementarUsoCupom(cupom);

        assertEquals(usosIniciais + 1, cupom.getUsosAtuais());
        verify(cupomRepository).save(cupom);
    }

    @Test
    void deveListarCuponsDisponiveisComSucesso() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Cupom> cuponsPage = new PageImpl<>(List.of(cupom));
        LocalDate hoje = LocalDate.now();
        when(cupomRepository.findCuponsDisponiveis(hoje, pageable)).thenReturn(cuponsPage);
        when(cupomMapper.toResponseDTO(cupom)).thenReturn(responseDTO);

        Page<CupomResponseDTO> resultado = cupomService.listarCuponsDisponiveis(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        verify(cupomRepository).findCuponsDisponiveis(hoje, pageable);
    }

    @Test
    void deveValidarCupomComSucesso() {
        LocalDate hoje = LocalDate.now();
        cupom.setUsosAtuais(50);
        cupom.setUsosMaximos(100);
        when(cupomRepository.findByCodigoAndAtivoTrueAndDataValida("DESCONTO10", true, hoje))
                .thenReturn(Optional.of(cupom));
        when(cupomMapper.toResponseDTO(cupom)).thenReturn(responseDTO);

        CupomResponseDTO resultado = cupomService.validarCupom("DESCONTO10");

        assertNotNull(resultado);
        assertEquals("DESCONTO10", resultado.getCodigo());
    }

    @Test
    void deveLancarExcecaoQuandoCupomEsgotado() {
        LocalDate hoje = LocalDate.now();
        cupom.setUsosAtuais(100);
        cupom.setUsosMaximos(100);
        when(cupomRepository.findByCodigoAndAtivoTrueAndDataValida("DESCONTO10", true, hoje))
                .thenReturn(Optional.of(cupom));

        assertThrows(IllegalStateException.class, () -> cupomService.validarCupom("DESCONTO10"));
    }
}

