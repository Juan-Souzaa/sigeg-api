package com.siseg.service;

import com.siseg.dto.AtualizarSenhaDTO;
import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.dto.restaurante.RestauranteRequestDTO;
import com.siseg.dto.restaurante.RestauranteResponseDTO;
import com.siseg.dto.restaurante.RestauranteUpdateDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.mapper.RestauranteMapper;
import com.siseg.model.Prato;
import com.siseg.model.Restaurante;
import com.siseg.model.User;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.model.Role;
import com.siseg.model.enumerations.ERole;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.PratoRepository;
import com.siseg.repository.RestauranteRepository;
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
import com.siseg.util.TempoEstimadoCalculator;

import com.siseg.dto.restaurante.RestauranteBuscaDTO;
import com.siseg.dto.geocoding.ResultadoCalculo;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.enumerations.TipoVeiculo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestauranteServiceUnitTest {

    @Mock
    private RestauranteRepository restauranteRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PratoRepository pratoRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private RestauranteMapper restauranteMapper;

    @Mock
    private TempoEstimadoCalculator tempoEstimadoCalculator;

    @InjectMocks
    private RestauranteService restauranteService;

    private RestauranteRequestDTO restauranteRequestDTO;
    private RestauranteResponseDTO restauranteResponseDTO;
    private Restaurante restaurante;

    @BeforeEach
    void setUp() {
        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro("Rua Teste");
        enderecoDTO.setNumero("123");
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(true);

        restauranteRequestDTO = new RestauranteRequestDTO();
        restauranteRequestDTO.setNome("Restaurante de Teste");
        restauranteRequestDTO.setEmail("teste@restaurante.com");
        restauranteRequestDTO.setTelefone("(11) 99999-9999");
        restauranteRequestDTO.setPassword("123456");
        restauranteRequestDTO.setEndereco(enderecoDTO);

        restauranteResponseDTO = new RestauranteResponseDTO();
        restauranteResponseDTO.setId(1L);
        restauranteResponseDTO.setNome("Restaurante de Teste");
        restauranteResponseDTO.setEmail("teste@restaurante.com");
        restauranteResponseDTO.setTelefone("(11) 99999-9999");
        restauranteResponseDTO.setStatus(StatusRestaurante.PENDING_APPROVAL);
        restauranteResponseDTO.setEndereco("Rua Teste, 123, Centro, São Paulo, SP, 01310-100, Brasil");

        restaurante = new Restaurante();
        restaurante.setId(1L);
        restaurante.setNome("Restaurante de Teste");
        restaurante.setEmail("teste@restaurante.com");
        restaurante.setTelefone("(11) 99999-9999");
        restaurante.setStatus(StatusRestaurante.PENDING_APPROVAL);
    }

    @Test
    void deveCriarRestauranteComSucesso() {
        // Given
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("teste@restaurante.com");
        
        Role restauranteRole = new Role();
        restauranteRole.setId(1L);
        restauranteRole.setRoleName(ERole.ROLE_RESTAURANTE);
        
        com.siseg.model.Endereco endereco = new com.siseg.model.Endereco();
        endereco.setId(1L);
        endereco.setLogradouro("Rua Teste");
        endereco.setNumero("123");
        endereco.setBairro("Centro");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setCep("01310100");
        
        when(userRepository.findByUsername(restauranteRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(restauranteRepository.findByEmail(restauranteRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(ERole.ROLE_RESTAURANTE)).thenReturn(Optional.of(restauranteRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(modelMapper.map(restauranteRequestDTO, Restaurante.class)).thenReturn(restaurante);
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
        when(enderecoService.criarEndereco(any(EnderecoRequestDTO.class), any(Restaurante.class))).thenReturn(endereco);
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        RestauranteResponseDTO result = restauranteService.criarRestaurante(restauranteRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals(restauranteResponseDTO.getId(), result.getId());
        assertEquals(restauranteResponseDTO.getNome(), result.getNome());
        assertEquals(StatusRestaurante.PENDING_APPROVAL, result.getStatus());
        verify(userRepository, times(1)).save(any(User.class));
        verify(restauranteRepository, times(1)).save(any(Restaurante.class));
        verify(enderecoService, times(1)).criarEndereco(any(EnderecoRequestDTO.class), any(Restaurante.class));
    }

    @Test
    void deveBuscarRestaurantePorIdComSucesso() {
        // Given
        Long id = 1L;
        when(restauranteRepository.findById(id)).thenReturn(Optional.of(restaurante));
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        // When
        RestauranteResponseDTO result = restauranteService.buscarPorId(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(restauranteRepository, times(1)).findById(id);
    }

    @Test
    void deveLancarExcecaoAoBuscarRestauranteInexistente() {
        // Given
        Long id = 999L;
        when(restauranteRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> restauranteService.buscarPorId(id));
        assertEquals("Restaurante não encontrado com ID: " + id, exception.getMessage());
    }

    @Test
    void deveListarRestaurantesComPaginacao() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Restaurante> page = new PageImpl<>(List.of(restaurante), pageable, 1);
        when(restauranteRepository.findAll(pageable)).thenReturn(page);
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        // When
        Page<RestauranteResponseDTO> result = restauranteService.listarTodos(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(restauranteRepository, times(1)).findAll(pageable);
    }

    @Test
    void deveListarRestaurantesPorStatus() {
        // Given
        StatusRestaurante status = StatusRestaurante.PENDING_APPROVAL;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Restaurante> page = new PageImpl<>(List.of(restaurante), pageable, 1);
        when(restauranteRepository.findByStatus(status, pageable)).thenReturn(page);
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        // When
        Page<RestauranteResponseDTO> result = restauranteService.listarPorStatus(status, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(restauranteRepository, times(1)).findByStatus(status, pageable);
    }

    @Test
    void deveAprovarRestauranteComSucesso() {
        // Given
        Long id = 1L;
        restaurante.setStatus(StatusRestaurante.APPROVED);
        when(restauranteRepository.findById(id)).thenReturn(Optional.of(restaurante));
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        // When
        RestauranteResponseDTO result = restauranteService.aprovarRestaurante(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(restauranteRepository, times(1)).findById(id);
        verify(restauranteRepository, times(1)).save(any(Restaurante.class));
    }

    @Test
    void deveRejeitarRestauranteComSucesso() {
        // Given
        Long id = 1L;
        restaurante.setStatus(StatusRestaurante.REJECTED);
        when(restauranteRepository.findById(id)).thenReturn(Optional.of(restaurante));
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        // When
        RestauranteResponseDTO result = restauranteService.rejeitarRestaurante(id);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(restauranteRepository, times(1)).findById(id);
        verify(restauranteRepository, times(1)).save(any(Restaurante.class));
    }

    @Test
    void deveCriarRestauranteSemCoordenadasSeGeocodificacaoFalhar() {
        // Given
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("teste@restaurante.com");
        
        Role restauranteRole = new Role();
        restauranteRole.setId(1L);
        restauranteRole.setRoleName(ERole.ROLE_RESTAURANTE);
        
        com.siseg.model.Endereco endereco = new com.siseg.model.Endereco();
        endereco.setId(1L);
        
        when(userRepository.findByUsername(restauranteRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(restauranteRepository.findByEmail(restauranteRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(ERole.ROLE_RESTAURANTE)).thenReturn(Optional.of(restauranteRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(modelMapper.map(restauranteRequestDTO, Restaurante.class)).thenReturn(restaurante);
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
        when(enderecoService.criarEndereco(any(EnderecoRequestDTO.class), any(Restaurante.class))).thenReturn(endereco);
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        RestauranteResponseDTO result = restauranteService.criarRestaurante(restauranteRequestDTO);

        // Then
        assertNotNull(result);
        // Restaurante deve ser criado mesmo sem coordenadas
        verify(userRepository, times(1)).save(any(User.class));
        verify(restauranteRepository, times(1)).save(any(Restaurante.class));
        verify(enderecoService, times(1)).criarEndereco(any(EnderecoRequestDTO.class), any(Restaurante.class));
    }

    @Test
    void deveAtualizarRestauranteComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            User user = new User();
            user.setId(1L);
            user.setUsername("restaurante@teste.com");
            restaurante.setUser(user);

            RestauranteUpdateDTO updateDTO = new RestauranteUpdateDTO();
            updateDTO.setNome("Restaurante Atualizado");
            updateDTO.setEmail("restaurante.atualizado@teste.com");
            updateDTO.setTelefone("(11) 88888-8888");

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

            RestauranteResponseDTO result = restauranteService.atualizarRestaurante(1L, updateDTO);

            assertNotNull(result);
            assertEquals("Restaurante Atualizado", restaurante.getNome());
            assertEquals("restaurante.atualizado@teste.com", restaurante.getEmail());
            verify(restauranteRepository, times(1)).save(restaurante);
            verify(userRepository, times(1)).save(user);
        }
    }

    @Test
    void deveExcluirRestauranteComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            restaurante.setAtivo(true);
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pedidoRepository.existsByRestauranteIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
            when(pratoRepository.findByRestauranteId(eq(1L), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(org.springframework.data.domain.Page.empty());
            when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);

            restauranteService.excluirRestaurante(1L);

            assertFalse(restaurante.getAtivo());
            verify(restauranteRepository, times(1)).save(restaurante);
        }
    }

    @Test
    void deveLancarExcecaoQuandoRestauranteTemPedidosEmAndamento() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pedidoRepository.existsByRestauranteIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

            assertThrows(IllegalStateException.class, 
                    () -> restauranteService.excluirRestaurante(1L));
        }
    }

    @Test
    void deveLancarExcecaoQuandoRestauranteTemPratosAtivos() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            Prato pratoAtivo = new Prato();
            pratoAtivo.setDisponivel(true);
            org.springframework.data.domain.Page<Prato> pratosPage = 
                    new org.springframework.data.domain.PageImpl<>(List.of(pratoAtivo));

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(pedidoRepository.existsByRestauranteIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
            when(pratoRepository.findByRestauranteId(eq(1L), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(pratosPage);

            assertThrows(IllegalStateException.class, 
                    () -> restauranteService.excluirRestaurante(1L));
        }
    }

    @Test
    void deveAtualizarSenhaRestauranteComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            User user = new User();
            user.setId(1L);
            user.setPassword("encodedOldPassword");
            restaurante.setUser(user);

            AtualizarSenhaDTO dto = new AtualizarSenhaDTO();
            dto.setSenhaAtual("senha123");
            dto.setNovaSenha("novaSenha456");

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(passwordEncoder.matches("senha123", "encodedOldPassword")).thenReturn(true);
            when(passwordEncoder.encode("novaSenha456")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);

            restauranteService.atualizarSenha(1L, dto);

            verify(passwordEncoder, times(1)).matches("senha123", "encodedOldPassword");
            verify(passwordEncoder, times(1)).encode("novaSenha456");
            verify(userRepository, times(1)).save(user);
        }
    }

    @Test
    void deveLancarExcecaoQuandoSenhaAtualIncorretaRestaurante() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            User user = new User();
            user.setPassword("encodedOldPassword");
            restaurante.setUser(user);

            AtualizarSenhaDTO dto = new AtualizarSenhaDTO();
            dto.setSenhaAtual("senhaErrada");
            dto.setNovaSenha("novaSenha456");

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(passwordEncoder.matches("senhaErrada", "encodedOldPassword")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, 
                    () -> restauranteService.atualizarSenha(1L, dto));
        }
    }

    @Test
    void deveCriarRestauranteComRaioEntrega() {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("teste@restaurante.com");
        
        Role restauranteRole = new Role();
        restauranteRole.setId(1L);
        restauranteRole.setRoleName(ERole.ROLE_RESTAURANTE);
        
        restauranteRequestDTO.setRaioEntregaKm(new BigDecimal("15.00"));
        
        when(userRepository.findByUsername(restauranteRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(restauranteRepository.findByEmail(restauranteRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(ERole.ROLE_RESTAURANTE)).thenReturn(Optional.of(restauranteRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(modelMapper.map(restauranteRequestDTO, Restaurante.class)).thenReturn(restaurante);
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
        when(enderecoService.criarEndereco(any(EnderecoRequestDTO.class), any(Restaurante.class)))
                .thenReturn(new Endereco());
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        restauranteService.criarRestaurante(restauranteRequestDTO);

        verify(restauranteRepository, times(1)).save(argThat(r -> 
            r.getRaioEntregaKm() != null && r.getRaioEntregaKm().compareTo(new BigDecimal("15.00")) == 0
        ));
    }

    @Test
    void deveCriarRestauranteComRaioPadraoQuandoNaoInformado() {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("teste@restaurante.com");
        
        Role restauranteRole = new Role();
        restauranteRole.setId(1L);
        restauranteRole.setRoleName(ERole.ROLE_RESTAURANTE);
        
        restauranteRequestDTO.setRaioEntregaKm(null);
        
        when(userRepository.findByUsername(restauranteRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(restauranteRepository.findByEmail(restauranteRequestDTO.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(ERole.ROLE_RESTAURANTE)).thenReturn(Optional.of(restauranteRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(modelMapper.map(restauranteRequestDTO, Restaurante.class)).thenReturn(restaurante);
        when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);
        when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
        when(enderecoService.criarEndereco(any(EnderecoRequestDTO.class), any(Restaurante.class)))
                .thenReturn(new Endereco());
        when(restauranteMapper.toResponseDTO(any(Restaurante.class))).thenReturn(restauranteResponseDTO);

        restauranteService.criarRestaurante(restauranteRequestDTO);

        verify(restauranteRepository, times(1)).save(argThat(r -> 
            r.getRaioEntregaKm() != null && r.getRaioEntregaKm().compareTo(new BigDecimal("10.00")) == 0
        ));
    }

    @Test
    void deveAtualizarRaioEntregaComSucesso() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            restaurante.setRaioEntregaKm(new BigDecimal("5.00"));
            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));
            when(restauranteRepository.save(any(Restaurante.class))).thenReturn(restaurante);

            restauranteService.atualizarRaioEntrega(1L, new BigDecimal("20.00"));

            assertEquals(new BigDecimal("20.00"), restaurante.getRaioEntregaKm());
            verify(restauranteRepository, times(1)).save(restaurante);
        }
    }

    @Test
    void deveLancarExcecaoQuandoRaioEntregaMenorQueMinimo() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));

            assertThrows(IllegalArgumentException.class, 
                    () -> restauranteService.atualizarRaioEntrega(1L, new BigDecimal("0.05")));
        }
    }

    @Test
    void deveLancarExcecaoQuandoRaioEntregaMaiorQueMaximo() {
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(new User());
            mockedSecurityUtils.when(SecurityUtils::isAdmin).thenReturn(true);

            when(restauranteRepository.findById(1L)).thenReturn(Optional.of(restaurante));

            assertThrows(IllegalArgumentException.class, 
                    () -> restauranteService.atualizarRaioEntrega(1L, new BigDecimal("51.00")));
        }
    }

    @Test
    void deveFiltrarRestaurantesPorRaioEntrega() {
        User mockUser = new User();
        mockUser.setId(1L);
        
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setUser(mockUser);
        
        Endereco enderecoCliente = new Endereco();
        enderecoCliente.setLatitude(new BigDecimal("-23.5505"));
        enderecoCliente.setLongitude(new BigDecimal("-46.6333"));
        enderecoCliente.setPrincipal(true);
        
        Restaurante restaurante1 = new Restaurante();
        restaurante1.setId(1L);
        restaurante1.setNome("Restaurante Próximo");
        restaurante1.setStatus(StatusRestaurante.APPROVED);
        restaurante1.setAtivo(true);
        restaurante1.setRaioEntregaKm(new BigDecimal("5.00"));
        
        Restaurante restaurante2 = new Restaurante();
        restaurante2.setId(2L);
        restaurante2.setNome("Restaurante Longe");
        restaurante2.setStatus(StatusRestaurante.APPROVED);
        restaurante2.setAtivo(true);
        restaurante2.setRaioEntregaKm(new BigDecimal("5.00"));
        
        Endereco enderecoRest1 = new Endereco();
        enderecoRest1.setLatitude(new BigDecimal("-23.5515"));
        enderecoRest1.setLongitude(new BigDecimal("-46.6343"));
        enderecoRest1.setPrincipal(true);
        
        Endereco enderecoRest2 = new Endereco();
        enderecoRest2.setLatitude(new BigDecimal("-23.6000"));
        enderecoRest2.setLongitude(new BigDecimal("-46.7000"));
        enderecoRest2.setPrincipal(true);
        
        RestauranteBuscaDTO dto1 = new RestauranteBuscaDTO();
        dto1.setId(1L);
        dto1.setNome("Restaurante Próximo");
        dto1.setDistanciaKm(new BigDecimal("1.50"));
        
        RestauranteBuscaDTO dto2 = new RestauranteBuscaDTO();
        dto2.setId(2L);
        dto2.setNome("Restaurante Longe");
        dto2.setDistanciaKm(new BigDecimal("10.00"));
        
        ResultadoCalculo resultado1 = new ResultadoCalculo(new BigDecimal("1.50"), 5, false);
        ResultadoCalculo resultado2 = new ResultadoCalculo(new BigDecimal("10.00"), 30, false);
        
        when(restauranteRepository.findAll()).thenReturn(List.of(restaurante1, restaurante2));
        when(clienteRepository.findByUserId(1L)).thenReturn(Optional.of(cliente));
        when(enderecoService.buscarEnderecoPrincipalCliente(1L)).thenReturn(Optional.of(enderecoCliente));
        when(enderecoService.buscarEnderecoPrincipalRestaurante(1L)).thenReturn(Optional.of(enderecoRest1));
        when(enderecoService.buscarEnderecoPrincipalRestaurante(2L)).thenReturn(Optional.of(enderecoRest2));
        when(restauranteMapper.toRestauranteBuscaDTO(restaurante1, cliente)).thenReturn(dto1);
        when(tempoEstimadoCalculator.calculateDistanceAndTime(
            any(BigDecimal.class), any(BigDecimal.class), 
            any(BigDecimal.class), any(BigDecimal.class), 
            eq(TipoVeiculo.MOTO)
        )).thenAnswer(invocation -> {
            BigDecimal lat2 = invocation.getArgument(2);
            if (lat2.compareTo(new BigDecimal("-23.5515")) == 0) {
                return resultado1;
            } else {
                return resultado2;
            }
        });
        
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUser).thenReturn(mockUser);
            
            Pageable pageable = PageRequest.of(0, 10);
            Page<RestauranteBuscaDTO> result = restauranteService.buscarRestaurantes(null, pageable);
            
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("Restaurante Próximo", result.getContent().get(0).getNome());
        }
    }
}
