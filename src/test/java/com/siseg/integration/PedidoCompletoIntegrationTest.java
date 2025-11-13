package com.siseg.integration;

import com.siseg.dto.carrinho.AplicarCupomRequestDTO;
import com.siseg.dto.carrinho.CarrinhoItemRequestDTO;
import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.model.*;
import com.siseg.model.enumerations.*;
import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.repository.*;
import com.siseg.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoCompletoIntegrationTest {

    @Autowired
    private CarrinhoService carrinhoService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private GanhosService ganhosService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private EntregadorRepository entregadorRepository;

    @Autowired
    private EnderecoService enderecoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private CupomRepository cupomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Cliente cliente;
    private Restaurante restaurante;
    private Prato prato;
    private Entregador entregador;
    private Cupom cupom;

    @BeforeEach
    void setUp() {
        cliente = criarCliente();
        restaurante = criarRestaurante();
        prato = criarPrato();
        entregador = criarEntregador();
        cupom = criarCupom();
        
        configurarAutenticacao(cliente.getUser());
    }
    
    private void configurarAutenticacao(User user) {
        UserAuthenticated userAuthenticated = new UserAuthenticated(user);
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userAuthenticated, null, userAuthenticated.getAuthorities());
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void deveExecutarFluxoCompletoCarrinhoPedidoPagamentoEntregaGanhos() {
        CarrinhoItemRequestDTO itemDTO = new CarrinhoItemRequestDTO();
        itemDTO.setPratoId(prato.getId());
        itemDTO.setQuantidade(2);

        carrinhoService.adicionarItem(itemDTO);

        AplicarCupomRequestDTO cupomDTO = new AplicarCupomRequestDTO();
        cupomDTO.setCodigo(cupom.getCodigo());
        carrinhoService.aplicarCupom(cupomDTO);

        var carrinho = carrinhoService.obterCarrinhoAtivo();
        assertNotNull(carrinho);
        assertNotNull(carrinho.getCupom());
        assertTrue(carrinho.getSubtotal().compareTo(BigDecimal.ZERO) > 0);

        PedidoRequestDTO pedidoDTO = new PedidoRequestDTO();
        pedidoDTO.setRestauranteId(restaurante.getId());
        pedidoDTO.setCarrinhoId(carrinho.getId());
        pedidoDTO.setMetodoPagamento(MetodoPagamento.PIX);
        // Não definir enderecoId - deve usar endereço principal do cliente

        var pedidoResponse = pedidoService.criarPedido(cliente.getId(), pedidoDTO);
        assertNotNull(pedidoResponse);
        assertEquals(StatusPedido.CREATED, pedidoResponse.getStatus());

        var pedido = pedidoRepository.findById(pedidoResponse.getId()).orElseThrow();
        assertTrue(pedido.getTotal().compareTo(BigDecimal.ZERO) > 0);

        pedidoService.confirmarPedido(pedido.getId());
        pedido = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.CONFIRMED, pedido.getStatus());

        configurarAutenticacao(restaurante.getUser());
        pedidoService.marcarComoPreparando(pedido.getId());
        pedido = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.PREPARING, pedido.getStatus());

        configurarAutenticacao(entregador.getUser());
        pedidoService.aceitarPedido(pedido.getId());
        pedido = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(entregador.getId(), pedido.getEntregador().getId());

        pedidoService.marcarSaiuEntrega(pedido.getId());
        pedido = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.OUT_FOR_DELIVERY, pedido.getStatus());

        pedidoService.marcarComoEntregue(pedido.getId());
        pedido = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertEquals(StatusPedido.DELIVERED, pedido.getStatus());
        assertNotNull(pedido.getTaxaPlataformaRestaurante());
        assertNotNull(pedido.getTaxaPlataformaEntregador());
        assertNotNull(pedido.getValorLiquidoRestaurante());
        assertNotNull(pedido.getValorLiquidoEntregador());

        var ganhosRestaurante = ganhosService.calcularGanhosRestaurante(restaurante.getId(), Periodo.HOJE);
        assertNotNull(ganhosRestaurante);
        assertTrue(ganhosRestaurante.getTotalPedidos() > 0);

        var ganhosEntregador = ganhosService.calcularGanhosEntregador(entregador.getId(), Periodo.HOJE);
        assertNotNull(ganhosEntregador);
        assertTrue(ganhosEntregador.getTotalEntregas() > 0);
    }

    @Test
    void deveAplicarDescontoCupomNoPedido() {
        CarrinhoItemRequestDTO itemDTO = new CarrinhoItemRequestDTO();
        itemDTO.setPratoId(prato.getId());
        itemDTO.setQuantidade(2);
        carrinhoService.adicionarItem(itemDTO);

        AplicarCupomRequestDTO cupomDTO = new AplicarCupomRequestDTO();
        cupomDTO.setCodigo(cupom.getCodigo());
        carrinhoService.aplicarCupom(cupomDTO);

        var carrinho = carrinhoService.obterCarrinhoAtivo();
        BigDecimal subtotal = carrinho.getSubtotal();
        BigDecimal desconto = carrinho.getDesconto();
        BigDecimal totalComDesconto = carrinho.getTotal();

        assertTrue(desconto.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(totalComDesconto.compareTo(subtotal) < 0);

        PedidoRequestDTO pedidoDTO = new PedidoRequestDTO();
        pedidoDTO.setRestauranteId(restaurante.getId());
        pedidoDTO.setCarrinhoId(carrinho.getId());
        pedidoDTO.setMetodoPagamento(MetodoPagamento.PIX);
        // Não definir enderecoId - deve usar endereço principal do cliente

        var pedidoResponse = pedidoService.criarPedido(cliente.getId(), pedidoDTO);
        var pedido = pedidoRepository.findById(pedidoResponse.getId()).orElseThrow();

        assertTrue(pedido.getTotal().compareTo(subtotal) < 0 || pedido.getTotal().equals(subtotal));
    }

    private Cliente criarCliente() {
        String email = "cliente" + System.currentTimeMillis() + "@teste.com";
        
        Role roleCliente = roleRepository.findByRoleName(ERole.ROLE_CLIENTE)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(ERole.ROLE_CLIENTE);
                    return roleRepository.save(newRole);
                });
        
        User user = new User();
        user.setUsername(email);
        user.setPassword("senha123");
        Set<Role> roles = new HashSet<>();
        roles.add(roleCliente);
        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        
        Cliente c = new Cliente();
        c.setNome("Cliente Teste");
        c.setEmail(email);
        c.setTelefone("(11) 99999-9999");
        c.setUser(savedUser);
        
        Cliente saved = clienteRepository.save(c);
        
        // Criar endereço
        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro("Rua do Cliente");
        enderecoDTO.setNumero("123");
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(true);
        enderecoService.criarEndereco(enderecoDTO, saved);
        
        return saved;
    }

    private Restaurante criarRestaurante() {
        String email = "restaurante" + System.currentTimeMillis() + "@teste.com";
        
        Role roleRestaurante = roleRepository.findByRoleName(ERole.ROLE_RESTAURANTE)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(ERole.ROLE_RESTAURANTE);
                    return roleRepository.save(newRole);
                });
        
        User user = new User();
        user.setUsername(email);
        user.setPassword("senha123");
        Set<Role> roles = new HashSet<>();
        roles.add(roleRestaurante);
        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        
        Restaurante r = new Restaurante();
        r.setNome("Restaurante Teste");
        r.setEmail(email);
        r.setTelefone("(11) 88888-8888");
        r.setStatus(StatusRestaurante.APPROVED);
        r.setUser(savedUser);
        
        Restaurante saved = restauranteRepository.save(r);
        
        // Criar endereço
        EnderecoRequestDTO enderecoDTO = new EnderecoRequestDTO();
        enderecoDTO.setLogradouro("Rua do Restaurante");
        enderecoDTO.setNumero("456");
        enderecoDTO.setBairro("Centro");
        enderecoDTO.setCidade("São Paulo");
        enderecoDTO.setEstado("SP");
        enderecoDTO.setCep("01310100");
        enderecoDTO.setPrincipal(true);
        enderecoService.criarEndereco(enderecoDTO, saved);
        
        return saved;
    }

    private Prato criarPrato() {
        Prato p = new Prato();
        p.setNome("Prato Teste");
        p.setPreco(new BigDecimal("25.50"));
        p.setCategoria(CategoriaMenu.MAIN);
        p.setDisponivel(true);
        p.setRestaurante(restaurante);
        return pratoRepository.save(p);
    }

    private Entregador criarEntregador() {
        Role roleEntregador = roleRepository.findByRoleName(ERole.ROLE_ENTREGADOR)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(ERole.ROLE_ENTREGADOR);
                    return roleRepository.save(newRole);
                });
        
        User user = new User();
        user.setUsername("entregador@teste.com");
        user.setPassword("senha123");
        Set<Role> roles = new HashSet<>();
        roles.add(roleEntregador);
        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        
        Entregador e = new Entregador();
        e.setNome("Entregador Teste");
        e.setEmail("entregador@teste.com");
        e.setTelefone("(11) 77777-7777");
        e.setCpf("12345678900");
        e.setPlacaVeiculo("ABC1234");
        e.setTipoVeiculo(TipoVeiculo.MOTO);
        e.setStatus(StatusEntregador.APPROVED);
        e.setUser(savedUser);
        
        return entregadorRepository.save(e);
    }

    private Cupom criarCupom() {
        Cupom c = new Cupom();
        c.setCodigo("TESTE10");
        c.setTipoDesconto(TipoDesconto.PERCENTUAL);
        c.setValorDesconto(new BigDecimal("10.00"));
        c.setValorMinimo(new BigDecimal("50.00"));
        c.setDataInicio(java.time.LocalDate.now());
        c.setDataFim(java.time.LocalDate.now().plusMonths(1));
        c.setAtivo(true);
        c.setUsosMaximos(100);
        c.setUsosAtuais(0);
        return cupomRepository.save(c);
    }
}

