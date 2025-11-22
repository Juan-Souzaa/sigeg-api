package com.siseg.service;

import com.siseg.dto.EnderecoCepResponseDTO;
import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.EnderecoResponseDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.mapper.EnderecoMapper;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.EnderecoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.validator.EnderecoValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EnderecoService {
    
    private final EnderecoRepository enderecoRepository;
    private final EnderecoValidator enderecoValidator;
    private final GeocodingService geocodingService;
    private final EnderecoMapper enderecoMapper;
    private final ClienteRepository clienteRepository;
    private final RestauranteRepository restauranteRepository;
    
    public EnderecoService(EnderecoRepository enderecoRepository, EnderecoValidator enderecoValidator, 
                          GeocodingService geocodingService, EnderecoMapper enderecoMapper,
                          ClienteRepository clienteRepository, RestauranteRepository restauranteRepository) {
        this.enderecoRepository = enderecoRepository;
        this.enderecoValidator = enderecoValidator;
        this.geocodingService = geocodingService;
        this.enderecoMapper = enderecoMapper;
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
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
    
    public Cliente buscarClienteParaEndereco(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));
        enderecoValidator.validateClienteOwnership(cliente);
        return cliente;
    }
    
    public Restaurante buscarRestauranteParaEndereco(Long restauranteId) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
        enderecoValidator.validateRestauranteOwnership(restaurante);
        return restaurante;
    }
    
    public List<EnderecoResponseDTO> listarEnderecosCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));
        enderecoValidator.validateClienteOwnership(cliente);
        
        List<Endereco> enderecos = enderecoRepository.findByClienteId(clienteId);
        return enderecos.stream()
                .map(enderecoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
    
    public List<EnderecoResponseDTO> listarEnderecosRestaurante(Long restauranteId) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
        enderecoValidator.validateRestauranteOwnership(restaurante);
        
        List<Endereco> enderecos = enderecoRepository.findByRestauranteId(restauranteId);
        return enderecos.stream()
                .map(enderecoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
    
    public EnderecoResponseDTO buscarEnderecoPorIdCliente(Long enderecoId, Long clienteId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com ID: " + enderecoId));
        
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));
        
        enderecoValidator.validateEnderecoPertenceAoCliente(endereco, clienteId, cliente);
        return enderecoMapper.toResponseDTO(endereco);
    }
    
    public EnderecoResponseDTO buscarEnderecoPorIdRestaurante(Long enderecoId, Long restauranteId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com ID: " + enderecoId));
        
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
        
        enderecoValidator.validateEnderecoPertenceAoRestaurante(endereco, restauranteId, restaurante);
        return enderecoMapper.toResponseDTO(endereco);
    }
    
    @Transactional
    public EnderecoResponseDTO atualizarEnderecoCliente(Long enderecoId, EnderecoRequestDTO dto, Long clienteId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com ID: " + enderecoId));
        
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));
        
        enderecoValidator.validateEnderecoPertenceAoCliente(endereco, clienteId, cliente);
        
        
        boolean camposGeocodificacaoAlterados = camposRelevantesParaGeocodificacaoAlterados(endereco, dto);
        
        boolean enderecoAlterado = atualizarCamposEndereco(endereco, dto);
        
        if (Boolean.TRUE.equals(dto.getPrincipal())) {
            desmarcarEnderecosPrincipaisCliente(clienteId);
            endereco.setPrincipal(true);
        }
        
       
        if (camposGeocodificacaoAlterados) {
            endereco.setLatitude(null);
            endereco.setLongitude(null);
        }
        
        Endereco saved = enderecoRepository.save(endereco);
        
       
        if (enderecoAlterado && (saved.getLatitude() == null || saved.getLongitude() == null)) {
            geocodingService.geocodeAddress(saved);
            saved = enderecoRepository.save(saved);
        }
        
        return enderecoMapper.toResponseDTO(saved);
    }
    
    @Transactional
    public EnderecoResponseDTO atualizarEnderecoRestaurante(Long enderecoId, EnderecoRequestDTO dto, Long restauranteId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com ID: " + enderecoId));
        
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
        
        enderecoValidator.validateEnderecoPertenceAoRestaurante(endereco, restauranteId, restaurante);
        
       
        boolean camposGeocodificacaoAlterados = camposRelevantesParaGeocodificacaoAlterados(endereco, dto);
        
        boolean enderecoAlterado = atualizarCamposEndereco(endereco, dto);
        
        if (Boolean.TRUE.equals(dto.getPrincipal())) {
            desmarcarEnderecosPrincipaisRestaurante(restauranteId);
            endereco.setPrincipal(true);
        }
        
       
        if (camposGeocodificacaoAlterados) {
            endereco.setLatitude(null);
            endereco.setLongitude(null);
        }
        
        Endereco saved = enderecoRepository.save(endereco);
        
       
        if (enderecoAlterado && (saved.getLatitude() == null || saved.getLongitude() == null)) {
            geocodingService.geocodeAddress(saved);
            saved = enderecoRepository.save(saved);
        }
        
        return enderecoMapper.toResponseDTO(saved);
    }
    
    @Transactional
    public void excluirEnderecoCliente(Long enderecoId, Long clienteId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com ID: " + enderecoId));
        
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + clienteId));
        
        enderecoValidator.validateEnderecoPertenceAoCliente(endereco, clienteId, cliente);
        
        long totalEnderecos = enderecoRepository.countByClienteId(clienteId);
        enderecoValidator.validatePodeExcluirEndereco(totalEnderecos, "cliente");
        
        boolean eraPrincipal = Boolean.TRUE.equals(endereco.getPrincipal());
        
        enderecoRepository.delete(endereco);
        
        if (eraPrincipal) {
            List<Endereco> enderecosRestantes = enderecoRepository.findByClienteId(clienteId);
            if (!enderecosRestantes.isEmpty()) {
                Endereco novoPrincipal = enderecosRestantes.get(0);
                novoPrincipal.setPrincipal(true);
                enderecoRepository.save(novoPrincipal);
            }
        }
    }
    
    @Transactional
    public void excluirEnderecoRestaurante(Long enderecoId, Long restauranteId) {
        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com ID: " + enderecoId));
        
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado com ID: " + restauranteId));
        
        enderecoValidator.validateEnderecoPertenceAoRestaurante(endereco, restauranteId, restaurante);
        
        long totalEnderecos = enderecoRepository.countByRestauranteId(restauranteId);
        enderecoValidator.validatePodeExcluirEndereco(totalEnderecos, "restaurante");
        
        boolean eraPrincipal = Boolean.TRUE.equals(endereco.getPrincipal());
        
        enderecoRepository.delete(endereco);
        
        if (eraPrincipal) {
            List<Endereco> enderecosRestantes = enderecoRepository.findByRestauranteId(restauranteId);
            if (!enderecosRestantes.isEmpty()) {
                Endereco novoPrincipal = enderecosRestantes.get(0);
                novoPrincipal.setPrincipal(true);
                enderecoRepository.save(novoPrincipal);
            }
        }
    }
    
    private boolean atualizarCamposEndereco(Endereco endereco, EnderecoRequestDTO dto) {
        boolean alterado = false;
        
        if (dto.getLogradouro() != null && !dto.getLogradouro().equals(endereco.getLogradouro())) {
            endereco.setLogradouro(dto.getLogradouro());
            alterado = true;
        }
        if (dto.getNumero() != null && !dto.getNumero().equals(endereco.getNumero())) {
            endereco.setNumero(dto.getNumero());
            alterado = true;
        }
        if (dto.getComplemento() != null && !dto.getComplemento().equals(endereco.getComplemento())) {
            endereco.setComplemento(dto.getComplemento());
            alterado = true;
        }
        if (dto.getBairro() != null && !dto.getBairro().equals(endereco.getBairro())) {
            endereco.setBairro(dto.getBairro());
            alterado = true;
        }
        if (dto.getCidade() != null && !dto.getCidade().equals(endereco.getCidade())) {
            endereco.setCidade(dto.getCidade());
            alterado = true;
        }
        if (dto.getEstado() != null && !dto.getEstado().toUpperCase().equals(endereco.getEstado())) {
            endereco.setEstado(dto.getEstado().toUpperCase());
            alterado = true;
        }
        if (dto.getCep() != null) {
            String cepLimpo = dto.getCep().replaceAll("[^0-9]", "");
            if (!cepLimpo.equals(endereco.getCep())) {
                endereco.setCep(cepLimpo);
                alterado = true;
            }
        }
        
        if (alterado) {
            enderecoValidator.validate(endereco);
        }
        
        return alterado;
    }
    
    private boolean camposRelevantesParaGeocodificacaoAlterados(Endereco endereco, EnderecoRequestDTO dto) {
        // Verifica se algum campo que afeta a geocodificação foi alterado
        // Complemento não afeta geocodificação, então não é verificado aqui
        return (dto.getLogradouro() != null && !dto.getLogradouro().equals(endereco.getLogradouro())) ||
               (dto.getNumero() != null && !dto.getNumero().equals(endereco.getNumero())) ||
               (dto.getBairro() != null && !dto.getBairro().equals(endereco.getBairro())) ||
               (dto.getCidade() != null && !dto.getCidade().equals(endereco.getCidade())) ||
               (dto.getEstado() != null && !dto.getEstado().toUpperCase().equals(endereco.getEstado())) ||
               (dto.getCep() != null && !dto.getCep().replaceAll("[^0-9]", "").equals(endereco.getCep()));
    }
    
    @Transactional(readOnly = true)
    public EnderecoCepResponseDTO buscarEnderecoPorCep(String cep) {
        return geocodingService.buscarEnderecoPorCep(cep)
                .orElseThrow(() -> new ResourceNotFoundException("CEP não encontrado: " + cep));
    }
}

