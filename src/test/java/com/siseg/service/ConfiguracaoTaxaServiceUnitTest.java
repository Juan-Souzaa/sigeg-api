package com.siseg.service;

import com.siseg.dto.configuracao.ConfiguracaoTaxaRequestDTO;
import com.siseg.dto.configuracao.ConfiguracaoTaxaResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.ConfiguracaoTaxa;
import com.siseg.model.enumerations.TipoTaxa;
import com.siseg.repository.ConfiguracaoTaxaRepository;
import com.siseg.validator.ConfiguracaoTaxaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracaoTaxaServiceUnitTest {

    @Mock
    private ConfiguracaoTaxaRepository configuracaoTaxaRepository;

    @Mock
    private ConfiguracaoTaxaValidator configuracaoTaxaValidator;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ConfiguracaoTaxaService configuracaoTaxaService;

    private ConfiguracaoTaxaRequestDTO requestDTO;
    private ConfiguracaoTaxa configuracaoTaxa;
    private ConfiguracaoTaxaResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new ConfiguracaoTaxaRequestDTO();
        requestDTO.setTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
        requestDTO.setPercentual(new BigDecimal("10.00"));

        configuracaoTaxa = new ConfiguracaoTaxa();
        configuracaoTaxa.setId(1L);
        configuracaoTaxa.setTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
        configuracaoTaxa.setPercentual(new BigDecimal("10.00"));
        configuracaoTaxa.setAtivo(true);

        responseDTO = new ConfiguracaoTaxaResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
        responseDTO.setPercentual(new BigDecimal("10.00"));
        responseDTO.setAtivo(true);
    }

    @Test
    void deveObterTaxaAtivaComSucesso() {
        when(configuracaoTaxaRepository.findByTipoTaxaAndAtivoTrue(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(Optional.of(configuracaoTaxa));

        ConfiguracaoTaxa resultado = configuracaoTaxaService.obterTaxaAtiva(TipoTaxa.TAXA_RESTAURANTE);

        assertNotNull(resultado);
        assertEquals(TipoTaxa.TAXA_RESTAURANTE, resultado.getTipoTaxa());
        verify(configuracaoTaxaValidator).validateTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
    }

    @Test
    void deveLancarExcecaoQuandoTaxaAtivaNaoEncontrada() {
        when(configuracaoTaxaRepository.findByTipoTaxaAndAtivoTrue(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                configuracaoTaxaService.obterTaxaAtiva(TipoTaxa.TAXA_RESTAURANTE));
    }

    @Test
    void deveObterPercentualTaxaAtivaComSucesso() {
        when(configuracaoTaxaRepository.findByTipoTaxaAndAtivoTrue(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(Optional.of(configuracaoTaxa));

        BigDecimal resultado = configuracaoTaxaService.obterPercentualTaxaAtiva(TipoTaxa.TAXA_RESTAURANTE);

        assertEquals(new BigDecimal("10.00"), resultado);
    }

    @Test
    void deveCriarConfiguracaoTaxaComSucesso() {
        when(configuracaoTaxaRepository.findByTipoTaxaOrderByCriadoEmDesc(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(List.of());
        when(configuracaoTaxaRepository.save(any(ConfiguracaoTaxa.class))).thenReturn(configuracaoTaxa);
        when(modelMapper.map(configuracaoTaxa, ConfiguracaoTaxaResponseDTO.class)).thenReturn(responseDTO);

        ConfiguracaoTaxaResponseDTO resultado = configuracaoTaxaService.criarConfiguracaoTaxa(requestDTO);

        assertNotNull(resultado);
        assertEquals(TipoTaxa.TAXA_RESTAURANTE, resultado.getTipoTaxa());
        verify(configuracaoTaxaValidator).validateTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
        verify(configuracaoTaxaValidator).validatePercentual(requestDTO.getPercentual());
        verify(configuracaoTaxaRepository).save(any(ConfiguracaoTaxa.class));
    }

    @Test
    void deveDesativarTaxasAnterioresAoCriarNova() {
        ConfiguracaoTaxa taxaAnterior = new ConfiguracaoTaxa();
        taxaAnterior.setId(2L);
        taxaAnterior.setTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
        taxaAnterior.setAtivo(true);

        when(configuracaoTaxaRepository.findByTipoTaxaOrderByCriadoEmDesc(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(List.of(taxaAnterior));
        when(configuracaoTaxaRepository.save(any(ConfiguracaoTaxa.class))).thenReturn(configuracaoTaxa);
        when(modelMapper.map(configuracaoTaxa, ConfiguracaoTaxaResponseDTO.class)).thenReturn(responseDTO);

        configuracaoTaxaService.criarConfiguracaoTaxa(requestDTO);

        assertFalse(taxaAnterior.getAtivo());
        verify(configuracaoTaxaRepository).saveAll(anyList());
    }

    @Test
    void deveListarHistoricoTaxasComSucesso() {
        when(configuracaoTaxaRepository.findByTipoTaxaOrderByCriadoEmDesc(TipoTaxa.TAXA_RESTAURANTE))
                .thenReturn(List.of(configuracaoTaxa));
        when(modelMapper.map(configuracaoTaxa, ConfiguracaoTaxaResponseDTO.class)).thenReturn(responseDTO);

        List<ConfiguracaoTaxaResponseDTO> resultado = configuracaoTaxaService.listarHistoricoTaxas(TipoTaxa.TAXA_RESTAURANTE);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(configuracaoTaxaValidator).validateTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
    }
}

