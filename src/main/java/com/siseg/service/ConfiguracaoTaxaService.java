package com.siseg.service;

import com.siseg.dto.configuracao.ConfiguracaoTaxaRequestDTO;
import com.siseg.dto.configuracao.ConfiguracaoTaxaResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.ConfiguracaoTaxa;
import com.siseg.model.enumerations.TipoTaxa;
import com.siseg.repository.ConfiguracaoTaxaRepository;
import com.siseg.validator.ConfiguracaoTaxaValidator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConfiguracaoTaxaService {

    private final ConfiguracaoTaxaRepository configuracaoTaxaRepository;
    private final ConfiguracaoTaxaValidator configuracaoTaxaValidator;
    private final ModelMapper modelMapper;

    public ConfiguracaoTaxaService(ConfiguracaoTaxaRepository configuracaoTaxaRepository,
                                    ConfiguracaoTaxaValidator configuracaoTaxaValidator,
                                    ModelMapper modelMapper) {
        this.configuracaoTaxaRepository = configuracaoTaxaRepository;
        this.configuracaoTaxaValidator = configuracaoTaxaValidator;
        this.modelMapper = modelMapper;
    }

    @Transactional(readOnly = true)
    public ConfiguracaoTaxa obterTaxaAtiva(TipoTaxa tipoTaxa) {
        configuracaoTaxaValidator.validateTipoTaxa(tipoTaxa);
        return configuracaoTaxaRepository.findByTipoTaxaAndAtivoTrue(tipoTaxa)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma taxa ativa encontrada para o tipo: " + tipoTaxa));
    }

    @Transactional(readOnly = true)
    public BigDecimal obterPercentualTaxaAtiva(TipoTaxa tipoTaxa) {
        ConfiguracaoTaxa configuracao = obterTaxaAtiva(tipoTaxa);
        return configuracao.getPercentual();
    }

    public ConfiguracaoTaxaResponseDTO criarConfiguracaoTaxa(ConfiguracaoTaxaRequestDTO dto) {
        configuracaoTaxaValidator.validateTipoTaxa(dto.getTipoTaxa());
        configuracaoTaxaValidator.validatePercentual(dto.getPercentual());

        desativarTaxasAnteriores(dto.getTipoTaxa());

        ConfiguracaoTaxa novaConfiguracao = criarNovaConfiguracao(dto);
        ConfiguracaoTaxa saved = configuracaoTaxaRepository.save(novaConfiguracao);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ConfiguracaoTaxaResponseDTO> listarHistoricoTaxas(TipoTaxa tipoTaxa) {
        configuracaoTaxaValidator.validateTipoTaxa(tipoTaxa);
        List<ConfiguracaoTaxa> configuracoes = configuracaoTaxaRepository.findByTipoTaxaOrderByCriadoEmDesc(tipoTaxa);
        return configuracoes.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private void desativarTaxasAnteriores(TipoTaxa tipoTaxa) {
        List<ConfiguracaoTaxa> taxasAtivas = configuracaoTaxaRepository.findByTipoTaxaOrderByCriadoEmDesc(tipoTaxa)
                .stream()
                .filter(ConfiguracaoTaxa::getAtivo)
                .collect(Collectors.toList());

        taxasAtivas.forEach(taxa -> taxa.setAtivo(false));
        configuracaoTaxaRepository.saveAll(taxasAtivas);
    }

    private ConfiguracaoTaxa criarNovaConfiguracao(ConfiguracaoTaxaRequestDTO dto) {
        ConfiguracaoTaxa configuracao = new ConfiguracaoTaxa();
        configuracao.setTipoTaxa(dto.getTipoTaxa());
        configuracao.setPercentual(dto.getPercentual());
        configuracao.setAtivo(true);
        return configuracao;
    }

    private ConfiguracaoTaxaResponseDTO toResponseDTO(ConfiguracaoTaxa configuracao) {
        return modelMapper.map(configuracao, ConfiguracaoTaxaResponseDTO.class);
    }
}

