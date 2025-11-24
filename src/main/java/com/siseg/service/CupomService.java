package com.siseg.service;

import com.siseg.dto.cupom.CupomRequestDTO;
import com.siseg.dto.cupom.CupomResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cupom;
import com.siseg.mapper.CupomMapper;
import com.siseg.repository.CupomRepository;
import com.siseg.validator.CupomValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class CupomService {

    private final CupomRepository cupomRepository;
    private final CupomValidator cupomValidator;
    private final CupomMapper cupomMapper;

    public CupomService(CupomRepository cupomRepository, CupomValidator cupomValidator, CupomMapper cupomMapper) {
        this.cupomRepository = cupomRepository;
        this.cupomValidator = cupomValidator;
        this.cupomMapper = cupomMapper;
    }

    public CupomResponseDTO criarCupom(CupomRequestDTO dto) {
        cupomValidator.validateCodigoUnico(dto.getCodigo());
        validarDatasCupom(dto.getDataInicio(), dto.getDataFim());

        Cupom cupom = criarCupomBasico(dto);
        Cupom saved = cupomRepository.save(cupom);
        return cupomMapper.toResponseDTO(saved);
    }

    public CupomResponseDTO atualizarCupom(Long id, CupomRequestDTO dto) {
        Cupom cupom = buscarCupomPorId(id);
        
        
        if (!cupom.getCodigo().equals(dto.getCodigo())) {
            cupomValidator.validateCodigoUnico(dto.getCodigo());
        }
        
        validarDatasCupom(dto.getDataInicio(), dto.getDataFim());
       
        cupom.setCodigo(dto.getCodigo());
        cupom.setTipoDesconto(dto.getTipoDesconto());
        cupom.setValorDesconto(dto.getValorDesconto());
        cupom.setValorMinimo(dto.getValorMinimo());
        cupom.setDataInicio(dto.getDataInicio());
        cupom.setDataFim(dto.getDataFim());
        cupom.setUsosMaximos(dto.getUsosMaximos());
       
        
        Cupom saved = cupomRepository.save(cupom);
        return cupomMapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Cupom buscarPorCodigo(String codigo) {
        LocalDate hoje = LocalDate.now();
        return cupomRepository.findByCodigoAndAtivoTrueAndDataValida(codigo, true, hoje)
                .orElseThrow(() -> new ResourceNotFoundException("Cupom não encontrado ou inválido"));
    }

    @Transactional(readOnly = true)
    public CupomResponseDTO buscarPorCodigoDTO(String codigo) {
        Cupom cupom = buscarPorCodigo(codigo);
        return cupomMapper.toResponseDTO(cupom);
    }

    @Transactional(readOnly = true)
    public Page<CupomResponseDTO> listarCuponsAtivos(Pageable pageable) {
        Page<Cupom> cupons = cupomRepository.findByAtivoTrue(pageable);
        return cupons.map(cupomMapper::toResponseDTO);
    }
    
    @Transactional(readOnly = true)
    public Page<CupomResponseDTO> listarCuponsDisponiveis(Pageable pageable) {
        LocalDate hoje = LocalDate.now();
        Page<Cupom> cupons = cupomRepository.findCuponsDisponiveis(hoje, pageable);
        return cupons.map(cupomMapper::toResponseDTO);
    }

    public CupomResponseDTO desativarCupom(Long id) {
        Cupom cupom = buscarCupomPorId(id);
        cupom.setAtivo(false);
        Cupom saved = cupomRepository.save(cupom);
        return cupomMapper.toResponseDTO(saved);
    }

    public CupomResponseDTO ativarCupom(Long id) {
        Cupom cupom = buscarCupomPorId(id);
        cupom.setAtivo(true);
        Cupom saved = cupomRepository.save(cupom);
        return cupomMapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<CupomResponseDTO> listarTodos(Pageable pageable) {
        Page<Cupom> cupons = cupomRepository.findAll(pageable);
        return cupons.map(cupomMapper::toResponseDTO);
    }

    public void incrementarUsoCupom(Cupom cupom) {
        cupom.setUsosAtuais(cupom.getUsosAtuais() + 1);
        cupomRepository.save(cupom);
    }
    
    @Transactional(readOnly = true)
    public CupomResponseDTO validarCupom(String codigo) {
        Cupom cupom = buscarPorCodigo(codigo);
        
        if (cupom.getUsosAtuais() >= cupom.getUsosMaximos()) {
            throw new IllegalStateException("Cupom esgotado");
        }
        
        return cupomMapper.toResponseDTO(cupom);
    }

    private Cupom buscarCupomPorId(Long id) {
        return cupomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cupom não encontrado com ID: " + id));
    }

    private void validarDatasCupom(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("Data de início deve ser anterior à data de fim");
        }
        if (dataFim.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Data de fim não pode ser anterior à data atual");
        }
    }

    private Cupom criarCupomBasico(CupomRequestDTO dto) {
        Cupom cupom = new Cupom();
        cupom.setCodigo(dto.getCodigo());
        cupom.setTipoDesconto(dto.getTipoDesconto());
        cupom.setValorDesconto(dto.getValorDesconto());
        cupom.setValorMinimo(dto.getValorMinimo());
        cupom.setDataInicio(dto.getDataInicio());
        cupom.setDataFim(dto.getDataFim());
        cupom.setUsosMaximos(dto.getUsosMaximos());
        cupom.setAtivo(true);
        return cupom;
    }
}

