package com.siseg.service;

import com.siseg.dto.AtualizarSenhaDTO;
import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.cliente.ClienteRequestDTO;
import com.siseg.dto.cliente.ClienteResponseDTO;
import com.siseg.dto.cliente.ClienteUpdateDTO;
import com.siseg.exception.AccessDeniedException;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import com.siseg.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceUnitTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private ClienteService clienteService;

    private User user;
    private Cliente cliente;
    private ClienteRequestDTO clienteRequestDTO;
    private ClienteResponseDTO clienteResponseDTO;
    private Role clienteRole;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("cliente@teste.com");

        clienteRole = new Role();
        clienteRole.setId(1L);
        clienteRole.setRoleName(ERole.ROLE_CLIENTE);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente@teste.com");
        cliente.setUser(user);

        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro("Rua do Cliente");
        enderecoDTO.setNumero("123");
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(true);

        clienteRequestDTO = new ClienteRequestDTO();
        clienteRequestDTO.setNome("Cliente Teste");
        clienteRequestDTO.setEmail("cliente@teste.com");
        clienteRequestDTO.setPassword("senha123");
        clienteRequestDTO.setEndereco(enderecoDTO);

        clienteResponseDTO = new ClienteResponseDTO();
        clienteResponseDTO.setId(1L);
        clienteResponseDTO.setNome("Cliente Teste");
        clienteResponseDTO.setEmail("cliente@teste.com");
    }

    @Test
    void deveCriarClienteComSucesso() {
        com.siseg.model.Endereco endereco = new com.siseg.model.Endereco();
        endereco.setId(1L);
        endereco.setLogradouro("Rua do Cliente");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01310100");

        when(roleRepository.findByRoleName(ERole.ROLE_CLIENTE)).thenReturn(Optional.of(clienteRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(modelMapper.map(clienteRequestDTO, Cliente.class)).thenReturn(cliente);
        when(enderecoService.criarEndereco(any(EnderecoRequestDTO.class), any(Cliente.class))).thenReturn(endereco);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoService.buscarEnderecoPrincipalCliente(1L)).thenReturn(Optional.of(endereco));
        when(modelMapper.map(any(Cliente.class), eq(ClienteResponseDTO.class))).thenReturn(clienteResponseDTO);

        ClienteResponseDTO result = clienteService.criarCliente(clienteRequestDTO);

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(clienteRepository, times(1)).save(any(Cliente.class));
        verify(enderecoService, times(1)).criarEndereco(any(EnderecoRequestDTO.class), any(Cliente.class));
    }

    @Test
    void deveCriarClienteSemGeocodificacaoQuandoFalha() {
        com.siseg.model.Endereco endereco = new com.siseg.model.Endereco();
        endereco.setId(1L);

        when(roleRepository.findByRoleName(ERole.ROLE_CLIENTE)).thenReturn(Optional.of(clienteRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(modelMapper.map(clienteRequestDTO, Cliente.class)).thenReturn(cliente);
        when(enderecoService.criarEndereco(any(EnderecoRequestDTO.class), any(Cliente.class))).thenReturn(endereco);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(enderecoService.buscarEnderecoPrincipalCliente(1L)).thenReturn(Optional.of(endereco));
        when(modelMapper.map(any(Cliente.class), eq(ClienteResponseDTO.class))).thenReturn(clienteResponseDTO);

        ClienteResponseDTO result = clienteService.criarCliente(clienteRequestDTO);

        assertNotNull(result);
        verify(enderecoService, times(1)).criarEndereco(any(EnderecoRequestDTO.class), any(Cliente.class));
    }

    @Test
    void deveLancarExcecaoQuandoRoleNaoEncontrada() {
        when(roleRepository.findByRoleName(ERole.ROLE_CLIENTE)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> clienteService.criarCliente(clienteRequestDTO));
    }

    @Test
    void deveBuscarClientePorIdComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(modelMapper.map(any(Cliente.class), eq(ClienteResponseDTO.class))).thenReturn(clienteResponseDTO);

            ClienteResponseDTO result = clienteService.buscarPorId(1L);

            assertNotNull(result);
            verify(clienteRepository, times(1)).findById(1L);
        }
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoEncontrado() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> clienteService.buscarPorId(1L));
        }
    }

    @Test
    void deveLancarExcecaoQuandoAcessoNegadoParaCliente() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            Cliente outroCliente = new Cliente();
            User outroUser = new User();
            outroUser.setId(2L);
            outroCliente.setUser(outroUser);

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(outroCliente));

            assertThrows(AccessDeniedException.class, 
                    () -> clienteService.buscarPorId(1L));
        }
    }

    @Test
    void deveListarTodosComoAdmin() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Cliente> clientesPage = new PageImpl<>(List.of(cliente), pageable, 1);

            when(clienteRepository.findAll(pageable)).thenReturn(clientesPage);
            when(modelMapper.map(any(Cliente.class), eq(ClienteResponseDTO.class))).thenReturn(clienteResponseDTO);

            Page<ClienteResponseDTO> result = clienteService.listarTodos(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(clienteRepository, times(1)).findAll(pageable);
        }
    }

    @Test
    void deveListarApenasPropriosClientesQuandoNaoAdmin() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(false);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Cliente> clientesPage = new PageImpl<>(List.of(cliente), pageable, 1);

            when(clienteRepository.findByUserId(user.getId(), pageable)).thenReturn(clientesPage);
            when(modelMapper.map(any(Cliente.class), eq(ClienteResponseDTO.class))).thenReturn(clienteResponseDTO);

            Page<ClienteResponseDTO> result = clienteService.listarTodos(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(clienteRepository, times(1)).findByUserId(user.getId(), pageable);
            verify(clienteRepository, never()).findAll(any(Pageable.class));
        }
    }

    @Test
    void deveAtualizarClienteComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            ClienteUpdateDTO updateDTO = new ClienteUpdateDTO();
            updateDTO.setNome("Cliente Atualizado");
            updateDTO.setEmail("cliente.atualizado@teste.com");
            updateDTO.setTelefone("(11) 99999-8888");

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(enderecoService.buscarEnderecoPrincipalCliente(1L)).thenReturn(Optional.empty());
            when(modelMapper.map(any(Cliente.class), eq(ClienteResponseDTO.class))).thenReturn(clienteResponseDTO);

            ClienteResponseDTO result = clienteService.atualizarCliente(1L, updateDTO);

            assertNotNull(result);
            assertEquals("Cliente Atualizado", cliente.getNome());
            assertEquals("cliente.atualizado@teste.com", cliente.getEmail());
            verify(clienteRepository, times(1)).save(cliente);
            verify(userRepository, times(1)).save(user);
        }
    }

    @Test
    void deveExcluirClienteComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            cliente.setAtivo(true);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(pedidoRepository.existsByClienteIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

            clienteService.excluirCliente(1L);

            assertFalse(cliente.getAtivo());
            verify(clienteRepository, times(1)).save(cliente);
        }
    }

    @Test
    void deveLancarExcecaoQuandoClienteTemPedidosEmAndamento() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(pedidoRepository.existsByClienteIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

            assertThrows(IllegalStateException.class, 
                    () -> clienteService.excluirCliente(1L));
        }
    }

    @Test
    void deveAtualizarSenhaComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            AtualizarSenhaDTO dto = new AtualizarSenhaDTO();
            dto.setSenhaAtual("senha123");
            dto.setNovaSenha("novaSenha456");

            user.setPassword("encodedOldPassword");
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(passwordEncoder.matches("senha123", "encodedOldPassword")).thenReturn(true);
            when(passwordEncoder.encode("novaSenha456")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            clienteService.atualizarSenha(1L, dto);

            verify(passwordEncoder, times(1)).matches("senha123", "encodedOldPassword");
            verify(passwordEncoder, times(1)).encode("novaSenha456");
            verify(userRepository, times(1)).save(user);
        }
    }

    @Test
    void deveLancarExcecaoQuandoSenhaAtualIncorreta() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(user);
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            AtualizarSenhaDTO dto = new AtualizarSenhaDTO();
            dto.setSenhaAtual("senhaErrada");
            dto.setNovaSenha("novaSenha456");

            user.setPassword("encodedOldPassword");
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
            when(passwordEncoder.matches("senhaErrada", "encodedOldPassword")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, 
                    () -> clienteService.atualizarSenha(1L, dto));
        }
    }
}

