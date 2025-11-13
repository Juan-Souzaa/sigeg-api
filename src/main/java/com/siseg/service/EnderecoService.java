package com.siseg.service;

import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.repository.EnderecoRepository;
import com.siseg.validator.EnderecoValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EnderecoService {
    
    private final EnderecoRepository enderecoRepository;
    private final EnderecoValidator enderecoValidator;
    private final GeocodingService geocodingService;
    
    public EnderecoService(EnderecoRepository enderecoRepository, EnderecoValidator enderecoValidator, GeocodingService geocodingService) {
        this.enderecoRepository = enderecoRepository;
        this.enderecoValidator = enderecoValidator;
        this.geocodingService = geocodingService;
    }
    
    @Transactional
    public Endereco criarEndereco(EnderecoRequestDTO dto, Cliente cliente) {
        Endereco endereco = validarEConverter(dto);
        endereco.setCliente(cliente);
        
        // Se for marcado como principal, desmarcar outros
        if (Boolean.TRUE.equals(dto.getPrincipal())) {
            desmarcarEnderecosPrincipaisCliente(cliente.getId());
            endereco.setPrincipal(true);
        } else {
            // Se for o primeiro endereço do cliente, marcar como principal
            List<Endereco> enderecosExistentes = enderecoRepository.findByClienteId(cliente.getId());
            if (enderecosExistentes.isEmpty()) {
                endereco.setPrincipal(true);
            }
        }
        
        Endereco saved = enderecoRepository.save(endereco);
        geocodingService.geocodeAddress(saved);
        return enderecoRepository.save(saved);
    }
    
    @Transactional
    public Endereco criarEndereco(EnderecoRequestDTO dto, Restaurante restaurante) {
        Endereco endereco = validarEConverter(dto);
        endereco.setRestaurante(restaurante);
        
        // Se for marcado como principal, desmarcar outros
        if (Boolean.TRUE.equals(dto.getPrincipal())) {
            desmarcarEnderecosPrincipaisRestaurante(restaurante.getId());
            endereco.setPrincipal(true);
        } else {
            // Se for o primeiro endereço do restaurante, marcar como principal
            List<Endereco> enderecosExistentes = enderecoRepository.findByRestauranteId(restaurante.getId());
            if (enderecosExistentes.isEmpty()) {
                endereco.setPrincipal(true);
            }
        }
        
        Endereco saved = enderecoRepository.save(endereco);
        geocodingService.geocodeAddress(saved);
        return enderecoRepository.save(saved);
    }
    
    @Transactional
    public void definirEnderecoPrincipalCliente(Long enderecoId, Long clienteId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado"));
        
        if (endereco.getCliente() == null || !endereco.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Endereço não pertence ao cliente");
        }
        
        desmarcarEnderecosPrincipaisCliente(clienteId);
        endereco.setPrincipal(true);
        enderecoRepository.save(endereco);
    }
    
    @Transactional
    public void definirEnderecoPrincipalRestaurante(Long enderecoId, Long restauranteId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado"));
        
        if (endereco.getRestaurante() == null || !endereco.getRestaurante().getId().equals(restauranteId)) {
            throw new IllegalArgumentException("Endereço não pertence ao restaurante");
        }
        
        desmarcarEnderecosPrincipaisRestaurante(restauranteId);
        endereco.setPrincipal(true);
        enderecoRepository.save(endereco);
    }
    
    public Optional<Endereco> buscarEnderecoPrincipalCliente(Long clienteId) {
        return enderecoRepository.findByClienteIdAndPrincipal(clienteId, true);
    }
    
    public Optional<Endereco> buscarEnderecoPrincipalRestaurante(Long restauranteId) {
        return enderecoRepository.findByRestauranteIdAndPrincipal(restauranteId, true);
    }
    
    public Optional<Endereco> buscarEnderecoPorIdECliente(Long enderecoId, Long clienteId) {
        return enderecoRepository.findByIdAndClienteId(enderecoId, clienteId);
    }
    
    @Transactional
    public Endereco geocodificarESalvar(Endereco endereco) {
        geocodingService.geocodeAddress(endereco);
        return enderecoRepository.save(endereco);
    }
    
    public Endereco validarEConverter(EnderecoRequestDTO dto) {
        Endereco endereco = new Endereco();
        endereco.setLogradouro(dto.getLogradouro());
        endereco.setNumero(dto.getNumero());
        endereco.setComplemento(dto.getComplemento());
        endereco.setBairro(dto.getBairro());
        endereco.setCidade(dto.getCidade());
        endereco.setEstado(dto.getEstado().toUpperCase());
        
        // Limpar CEP (remover hífen e espaços)
        String cepLimpo = dto.getCep().replaceAll("[^0-9]", "");
        endereco.setCep(cepLimpo);
        
        endereco.setTipo(TipoEndereco.OUTRO);
        
        enderecoValidator.validate(endereco);
        
        return endereco;
    }
    
    private void desmarcarEnderecosPrincipaisCliente(Long clienteId) {
        List<Endereco> enderecos = enderecoRepository.findByClienteId(clienteId);
        enderecos.forEach(e -> {
            if (Boolean.TRUE.equals(e.getPrincipal())) {
                e.setPrincipal(false);
                enderecoRepository.save(e);
            }
        });
    }
    
    private void desmarcarEnderecosPrincipaisRestaurante(Long restauranteId) {
        List<Endereco> enderecos = enderecoRepository.findByRestauranteId(restauranteId);
        enderecos.forEach(e -> {
            if (Boolean.TRUE.equals(e.getPrincipal())) {
                e.setPrincipal(false);
                enderecoRepository.save(e);
            }
        });
    }
}

