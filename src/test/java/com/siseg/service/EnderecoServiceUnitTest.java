package com.siseg.service;

import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.EnderecoResponseDTO;
import com.siseg.mapper.EnderecoMapper;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.dto.EnderecoCepResponseDTO;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.EnderecoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.validator.EnderecoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnderecoServiceUnitTest {

    @Mock
    private EnderecoRepository enderecoRepository;

    @Mock
    private EnderecoValidator enderecoValidator;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private EnderecoMapper enderecoMapper;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private RestauranteRepository restauranteRepository;

    @InjectMocks
    private EnderecoService enderecoService;

    private Cliente cliente;
    private Restaurante restaurante;
    private Endereco endereco;
    private EnderecoRequestDTO enderecoRequestDTO;
    private EnderecoResponseDTO enderecoResponseDTO;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("cliente@teste.com");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setUser(user);

        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setNome("Restaurante Teste");
        restaurante.setUser(user);

        endereco = new Endereco();
        endereco.setId(1L);
        endereco.setLogradouro("Rua Teste");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01310100");
        endereco.setCliente(cliente);
        endereco.setPrincipal(false);

        enderecoRequestDTO = new EnderecoRequestDTO();
        enderecoRequestDTO.setLogradouro("Rua Teste");
        enderecoRequestDTO.setNumero("123");
        enderecoRequestDTO.setBairro("Centro");
        enderecoRequestDTO.setCidade("São Paulo");
        enderecoRequestDTO.setEstado("SP");
        enderecoRequestDTO.setCep("01310-100");
        enderecoRequestDTO.setPrincipal(false);

        enderecoResponseDTO = new EnderecoResponseDTO();
        enderecoResponseDTO.setId(1L);
        enderecoResponseDTO.setLogradouro("Rua Teste");
        enderecoResponseDTO.setNumero("123");
        enderecoResponseDTO.setBairro("Centro");
        enderecoResponseDTO.setCidade("São Paulo");
        enderecoResponseDTO.setEstado("SP");
        enderecoResponseDTO.setCep("01310-100");
    }

    @Test
    void deveListarEnderecosCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.findByClienteId(1L)).thenReturn(Arrays.asList(endereco));
        when(enderecoMapper.toResponseDTO(any(Endereco.class))).thenReturn(enderecoResponseDTO);
        doNothing().when(enderecoValidator).validateClienteOwnership(any(Cliente.class));

        List<EnderecoResponseDTO> result = enderecoService.listarEnderecosCliente(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(clienteRepository).findById(1L);
        verify(enderecoRepository).findByClienteId(1L);
        verify(enderecoMapper).toResponseDTO(endereco);
        verify(enderecoValidator).validateClienteOwnership(cliente);
    }

    @Test
    void deveListarEnderecosRestaurante() {
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
        when(enderecoRepository.findByRestauranteId(1L)).thenReturn(Arrays.asList(endereco));
        when(enderecoMapper.toResponseDTO(any(Endereco.class))).thenReturn(enderecoResponseDTO);
        doNothing().when(enderecoValidator).validateRestauranteOwnership(any(Restaurante.class));

        List<EnderecoResponseDTO> result = enderecoService.listarEnderecosRestaurante(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(restauranteRepository).findById(1L);
        verify(enderecoRepository).findByRestauranteId(1L);
        verify(enderecoMapper).toResponseDTO(endereco);
        verify(enderecoValidator).validateRestauranteOwnership(restaurante);
    }

    @Test
    void deveBuscarEnderecoPorIdCliente() {
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoMapper.toResponseDTO(endereco)).thenReturn(enderecoResponseDTO);
        doNothing().when(enderecoValidator).validateEnderecoPertenceAoCliente(any(Endereco.class), eq(1L), any(Cliente.class));

        EnderecoResponseDTO result = enderecoService.buscarEnderecoPorIdCliente(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(enderecoRepository).findById(1L);
        verify(clienteRepository).findById(1L);
        verify(enderecoMapper).toResponseDTO(endereco);
        verify(enderecoValidator).validateEnderecoPertenceAoCliente(endereco, 1L, cliente);
    }

    @Test
    void deveLancarExcecaoQuandoEnderecoNaoPertenceAoCliente() {
        Endereco enderecoOutroCliente = new Endereco();
        enderecoOutroCliente.setId(1L);
        Cliente outroCliente = new Cliente();
        outroCliente.setId(2L);
        enderecoOutroCliente.setCliente(outroCliente);

        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(enderecoOutroCliente));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        doThrow(new IllegalArgumentException("Endereço não pertence ao cliente"))
                .when(enderecoValidator).validateEnderecoPertenceAoCliente(any(), eq(1L), any());

        assertThrows(IllegalArgumentException.class, () -> {
            enderecoService.buscarEnderecoPorIdCliente(1L, 1L);
        });
    }

    @Test
    void deveAtualizarEnderecoCliente() {
        endereco.setLatitude(new java.math.BigDecimal("-23.5505"));
        endereco.setLongitude(new java.math.BigDecimal("-46.6333"));
        
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.save(any(Endereco.class))).thenReturn(endereco);
        when(enderecoMapper.toResponseDTO(any(Endereco.class))).thenReturn(enderecoResponseDTO);
        doNothing().when(enderecoValidator).validateEnderecoPertenceAoCliente(any(Endereco.class), eq(1L), any(Cliente.class));
        doNothing().when(enderecoValidator).validate(any(Endereco.class));

        EnderecoRequestDTO dtoAtualizado = new EnderecoRequestDTO();
        dtoAtualizado.setLogradouro("Rua Atualizada");
        dtoAtualizado.setNumero("456");
        dtoAtualizado.setBairro("Novo Bairro");
        dtoAtualizado.setCidade("São Paulo");
        dtoAtualizado.setEstado("SP");
        dtoAtualizado.setCep("01310-200");
        dtoAtualizado.setPrincipal(false);

        EnderecoResponseDTO result = enderecoService.atualizarEnderecoCliente(1L, dtoAtualizado, 1L);

        assertNotNull(result);
        verify(enderecoRepository, times(1)).save(any(Endereco.class));
        verify(enderecoValidator).validateEnderecoPertenceAoCliente(endereco, 1L, cliente);
    }

    @Test
    void deveExcluirEnderecoCliente() {
        endereco.setPrincipal(false);
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.countByClienteId(1L)).thenReturn(2L);
        doNothing().when(enderecoValidator).validateEnderecoPertenceAoCliente(any(Endereco.class), eq(1L), any(Cliente.class));
        doNothing().when(enderecoValidator).validatePodeExcluirEndereco(eq(2L), anyString());

        enderecoService.excluirEnderecoCliente(1L, 1L);

        verify(enderecoRepository).delete(endereco);
        verify(enderecoValidator).validateEnderecoPertenceAoCliente(endereco, 1L, cliente);
        verify(enderecoValidator).validatePodeExcluirEndereco(2L, "cliente");
    }

    @Test
    void deveLancarExcecaoAoExcluirUnicoEnderecoCliente() {
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.countByClienteId(1L)).thenReturn(1L);
        doNothing().when(enderecoValidator).validateEnderecoPertenceAoCliente(any(Endereco.class), eq(1L), any(Cliente.class));
        doThrow(new IllegalArgumentException("Não é possível excluir o único endereço do cliente"))
                .when(enderecoValidator).validatePodeExcluirEndereco(eq(1L), anyString());

        assertThrows(IllegalArgumentException.class, () -> {
            enderecoService.excluirEnderecoCliente(1L, 1L);
        });

        verify(enderecoRepository, never()).delete(any());
    }

    @Test
    void deveMarcarNovoPrincipalAoExcluirEnderecoPrincipal() {
        Endereco enderecoPrincipal = new Endereco();
        enderecoPrincipal.setId(1L);
        enderecoPrincipal.setPrincipal(true);
        enderecoPrincipal.setCliente(cliente);

        Endereco enderecoSecundario = new Endereco();
        enderecoSecundario.setId(2L);
        enderecoSecundario.setPrincipal(false);
        enderecoSecundario.setCliente(cliente);

        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(enderecoPrincipal));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoRepository.countByClienteId(1L)).thenReturn(2L);
        when(enderecoRepository.findByClienteId(1L)).thenReturn(Arrays.asList(enderecoSecundario));
        when(enderecoRepository.save(any(Endereco.class))).thenReturn(enderecoSecundario);
        doNothing().when(enderecoValidator).validateEnderecoPertenceAoCliente(any(Endereco.class), eq(1L), any(Cliente.class));
        doNothing().when(enderecoValidator).validatePodeExcluirEndereco(eq(2L), anyString());

        enderecoService.excluirEnderecoCliente(1L, 1L);

        verify(enderecoRepository).delete(enderecoPrincipal);
        verify(enderecoRepository).save(enderecoSecundario);
        assertTrue(enderecoSecundario.getPrincipal());
    }

    @Test
    void deveBuscarEnderecoPorCepComSucesso() {
        EnderecoCepResponseDTO dto = new EnderecoCepResponseDTO();
        dto.setLogradouro("Rua Teste");
        dto.setBairro("Centro");
        dto.setCidade("São Paulo");
        dto.setEstado("SP");
        dto.setCep("01310100");

        when(geocodingService.buscarEnderecoPorCep("01310100")).thenReturn(java.util.Optional.of(dto));

        EnderecoCepResponseDTO result = enderecoService.buscarEnderecoPorCep("01310100");

        assertNotNull(result);
        assertEquals("Rua Teste", result.getLogradouro());
        assertEquals("São Paulo", result.getCidade());
        assertEquals("SP", result.getEstado());
        verify(geocodingService, times(1)).buscarEnderecoPorCep("01310100");
    }

    @Test
    void deveLancarExcecaoQuandoCepNaoEncontrado() {
        when(geocodingService.buscarEnderecoPorCep("00000000")).thenReturn(java.util.Optional.empty());

        assertThrows(com.siseg.exception.ResourceNotFoundException.class, 
                () -> enderecoService.buscarEnderecoPorCep("00000000"));
    }

}

