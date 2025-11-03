package com.siseg.service;

import com.siseg.dto.prato.PratoRequestDTO;
import com.siseg.dto.prato.PratoResponseDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Prato;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
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

@Service
@Transactional
public class PratoService {
    
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
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
        
        // Valida se o usuário é dono do restaurante
        validateRestauranteOwnership(restaurante);
        
        Prato prato = modelMapper.map(dto, Prato.class);
        prato.setRestaurante(restaurante);
        
        // Upload da foto se fornecida
        if (dto.getFoto() != null && !dto.getFoto().isEmpty()) {
            String fotoUrl = salvarFoto(dto.getFoto());
            prato.setFotoUrl(fotoUrl);
        }
        
        Prato saved = pratoRepository.save(prato);
        return modelMapper.map(saved, PratoResponseDTO.class);
    }
    
    public PratoResponseDTO atualizarPrato(Long id, PratoRequestDTO dto) {
        Prato prato = pratoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com ID: " + id));
        
        // Valida se o usuário é dono do restaurante do prato
        validateRestauranteOwnership(prato.getRestaurante());
        
        // Registrar alterações antes de atualizar
        registrarAlteracao(prato, "nome", prato.getNome(), dto.getNome());
        registrarAlteracao(prato, "descricao", prato.getDescricao(), dto.getDescricao());
        registrarAlteracao(prato, "preco", prato.getPreco().toString(), dto.getPreco().toString());
        registrarAlteracao(prato, "disponivel", prato.getDisponivel().toString(), dto.getDisponivel().toString());
        
        modelMapper.map(dto, prato);
        
        // Upload da nova foto se fornecida
        if (dto.getFoto() != null && !dto.getFoto().isEmpty()) {
            String fotoUrl = salvarFoto(dto.getFoto());
            prato.setFotoUrl(fotoUrl);
        }
        
        Prato saved = pratoRepository.save(prato);
        return modelMapper.map(saved, PratoResponseDTO.class);
    }
    
    public PratoResponseDTO alternarDisponibilidade(Long id) {
        Prato prato = pratoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prato não encontrado com ID: " + id));
        
        // Valida se o usuário é dono do restaurante do prato
        validateRestauranteOwnership(prato.getRestaurante());
        
        Boolean antigoStatus = prato.getDisponivel();
        prato.setDisponivel(!antigoStatus);
        
        registrarAlteracao(prato, "disponivel", antigoStatus.toString(), prato.getDisponivel().toString());
        
        Prato saved = pratoRepository.save(prato);
        return modelMapper.map(saved, PratoResponseDTO.class);
    }
    
    private void validateRestauranteOwnership(Restaurante restaurante) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Admin pode acessar qualquer restaurante
        if (SecurityUtils.isAdmin()) {
            return;
        }
        
        // Verifica se o restaurante pertence ao usuário autenticado
        if (restaurante.getUser() == null || !restaurante.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Você não tem permissão para gerenciar pratos deste restaurante");
        }
    }
    
    @Transactional(readOnly = true)
    public Page<PratoResponseDTO> listarPorRestaurante(Long restauranteId, CategoriaMenu categoria, Boolean disponivel, Pageable pageable) {
        Page<Prato> pratos;
        
        if (categoria != null && disponivel != null) {
            pratos = pratoRepository.findByRestauranteIdAndCategoriaAndDisponivel(restauranteId, categoria, disponivel, pageable);
        } else if (disponivel != null) {
            pratos = pratoRepository.findByRestauranteIdAndDisponivel(restauranteId, disponivel, pageable);
        } else if (categoria != null) {
            pratos = pratoRepository.findByRestauranteIdAndCategoria(restauranteId, categoria, pageable);
        } else {
            pratos = pratoRepository.findByRestauranteId(restauranteId, pageable);
        }
        
        return pratos.map(p -> modelMapper.map(p, PratoResponseDTO.class));
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
            
            System.out.println("Prato ID " + prato.getId() + " - Campo: " + campo + 
                             " - Antigo: " + valorAntigo + " - Novo: " + valorNovo);
        }
    }
}
