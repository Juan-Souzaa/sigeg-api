package com.siseg.service;

import com.siseg.dto.prato.PratoRequestDTO;
import com.siseg.dto.prato.PratoResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Prato;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.repository.PratoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.util.SecurityUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

@Service
@Transactional
public class PratoService {
    
    private static final Logger logger = Logger.getLogger(PratoService.class.getName());
    
    @Value("${app.storage.upload-dir:uploads}")
    private String uploadDir;
    
    private final PratoRepository pratoRepository;
    private final RestauranteRepository restauranteRepository;
    private final ModelMapper modelMapper;
    
    public PratoService(PratoRepository pratoRepository, RestauranteRepository restauranteRepository, ModelMapper modelMapper) {
        this.pratoRepository = pratoRepository;
        this.restauranteRepository = restauranteRepository;
        this.modelMapper = modelMapper;
    }
    
    public PratoResponseDTO criarPrato(Long restauranteId, PratoRequestDTO dto) {
        Restaurante restaurante = buscarRestaurante(restauranteId);
        SecurityUtils.validateRestauranteOwnership(restaurante);
        
        Prato prato = criarPratoBasico(restaurante, dto);
        
        if (temFoto(dto)) {
            prato.setFotoUrl(salvarFoto(dto.getFoto()));
        }
        
        Prato saved = pratoRepository.save(prato);
        return modelMapper.map(saved, PratoResponseDTO.class);
    }
    
    private Restaurante buscarRestaurante(Long restauranteId) {
        return restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
    }
    
    private Prato criarPratoBasico(Restaurante restaurante, PratoRequestDTO dto) {
        Prato prato = modelMapper.map(dto, Prato.class);
        prato.setRestaurante(restaurante);
        return prato;
    }
    
    private boolean temFoto(PratoRequestDTO dto) {
        return dto.getFoto() != null && !dto.getFoto().isEmpty();
    }
    
    public PratoResponseDTO atualizarPrato(Long id, PratoRequestDTO dto) {
        Prato prato = buscarPrato(id);
        SecurityUtils.validateRestauranteOwnership(prato.getRestaurante());
        
        registrarAlteracoes(prato, dto);
        modelMapper.map(dto, prato);
        
        if (temFoto(dto)) {
            prato.setFotoUrl(salvarFoto(dto.getFoto()));
        }
        
        Prato saved = pratoRepository.save(prato);
        return modelMapper.map(saved, PratoResponseDTO.class);
    }
    
    private Prato buscarPrato(Long id) {
        return pratoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com ID: " + id));
    }
    
    private void registrarAlteracoes(Prato prato, PratoRequestDTO dto) {
        registrarAlteracao(prato, "nome", prato.getNome(), dto.getNome());
        registrarAlteracao(prato, "descricao", prato.getDescricao(), dto.getDescricao());
        registrarAlteracao(prato, "preco", prato.getPreco().toString(), dto.getPreco().toString());
        registrarAlteracao(prato, "disponivel", prato.getDisponivel().toString(), dto.getDisponivel().toString());
    }
    
    public PratoResponseDTO alternarDisponibilidade(Long id) {
        Prato prato = buscarPrato(id);
        SecurityUtils.validateRestauranteOwnership(prato.getRestaurante());
        
        Boolean antigoStatus = prato.getDisponivel();
        prato.setDisponivel(!antigoStatus);
        registrarAlteracao(prato, "disponivel", antigoStatus.toString(), prato.getDisponivel().toString());
        
        Prato saved = pratoRepository.save(prato);
        return modelMapper.map(saved, PratoResponseDTO.class);
    }
    
    @Transactional(readOnly = true)
    public Page<PratoResponseDTO> listarPorRestaurante(Long restauranteId, CategoriaMenu categoria, Boolean disponivel, Pageable pageable) {
        Page<Prato> pratos = buscarPratosPorFiltros(restauranteId, categoria, disponivel, pageable);
        return pratos.map(p -> modelMapper.map(p, PratoResponseDTO.class));
    }
    
    private Page<Prato> buscarPratosPorFiltros(Long restauranteId, CategoriaMenu categoria, Boolean disponivel, Pageable pageable) {
        if (categoria != null && disponivel != null) {
            return pratoRepository.findByRestauranteIdAndCategoriaAndDisponivel(restauranteId, categoria, disponivel, pageable);
        }
        
        if (disponivel != null) {
            return pratoRepository.findByRestauranteIdAndDisponivel(restauranteId, disponivel, pageable);
        }
        
        if (categoria != null) {
            return pratoRepository.findByRestauranteIdAndCategoria(restauranteId, categoria, pageable);
        }
        
        return pratoRepository.findByRestauranteId(restauranteId, pageable);
    }
    
    private String salvarFoto(MultipartFile foto) {
        try {
            String nomeArquivo = UUID.randomUUID().toString() + "_" + foto.getOriginalFilename();
            Path diretorio = Paths.get(uploadDir, "menus");
            Files.createDirectories(diretorio);
            
            Path arquivo = diretorio.resolve(nomeArquivo);
            Files.copy(foto.getInputStream(), arquivo);
            
            return "/files/menus/" + nomeArquivo;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar foto: " + e.getMessage());
        }
    }
    
    private void registrarAlteracao(Prato prato, String campo, String valorAntigo, String valorNovo) {
        if (!valorAntigo.equals(valorNovo)) {
            logger.info(String.format("Prato ID %d - Campo: %s - Antigo: %s - Novo: %s", 
                prato.getId(), campo, valorAntigo, valorNovo));
        }
    }
}
