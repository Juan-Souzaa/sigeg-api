package com.siseg.config;

import com.siseg.dto.EnderecoRequestDTO;
import com.siseg.model.*;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.DisponibilidadeEntregador;
import com.siseg.model.enumerations.StatusEntregador;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.model.enumerations.TipoTaxa;
import com.siseg.model.enumerations.TipoVeiculo;
import com.siseg.repository.*;
import com.siseg.service.EnderecoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private CarrinhoRepository carrinhoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EntregadorRepository entregadorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EnderecoService enderecoService;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private ConfiguracaoTaxaRepository configuracaoTaxaRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Criar roles se não existirem
        if (roleRepository.count() == 0) {
            createRoles();
        }

        // Criar usuário admin se não existir
        if (userRepository.findByUsername("admin").isEmpty()) {
            createAdminUser();
        }

        // Criar clientes de exemplo se não existirem
        if (clienteRepository.count() == 0) {
            createSampleClients();
        }

        // Criar restaurante, prato, carrinho e pedido para testes
        if (restauranteRepository.count() == 0) {
            createTestRestaurantAndData();
        }

        // Criar entregador de teste se não existir
        if (entregadorRepository.count() == 0) {
            createTestEntregador();
        }

        // Criar taxas iniciais se não existirem
        createInitialTaxas();
    }

    private void createRoles() {
        Role adminRole = new Role();
        adminRole.setRoleName(ERole.ROLE_ADMIN);
        roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setRoleName(ERole.ROLE_USER);
        roleRepository.save(userRole);

        Role restauranteRole = new Role();
        restauranteRole.setRoleName(ERole.ROLE_RESTAURANTE);
        roleRepository.save(restauranteRole);

        Role entregadorRole = new Role();
        entregadorRole.setRoleName(ERole.ROLE_ENTREGADOR);
        roleRepository.save(entregadorRole);

        Role clienteRole = new Role();
        clienteRole.setRoleName(ERole.ROLE_CLIENTE);
        roleRepository.save(clienteRole);

        System.out.println("✅ Roles criados com sucesso!");
    }

    private void createAdminUser() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));

        Set<Role> roles = new HashSet<>();
        Role adminRole = roleRepository.findByRoleName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Role ADMIN não encontrado"));
        roles.add(adminRole);

        admin.setRoles(roles);
        userRepository.save(admin);

        System.out.println("✅ Usuário admin criado com sucesso!");
        System.out.println("   Username: admin");
        System.out.println("   Password: admin123");
    }

    private void createSampleClients() {
        // Cliente 1
        User user1 = new User();
        user1.setUsername("joao.silva@email.com");
        user1.setPassword(passwordEncoder.encode("123456"));
        
        Set<Role> roles1 = new HashSet<>();
        Role clienteRole = roleRepository.findByRoleName(ERole.ROLE_CLIENTE)
                .orElseThrow(() -> new RuntimeException("Role CLIENTE não encontrado"));
        roles1.add(clienteRole);
        user1.setRoles(roles1);
        
        User savedUser1 = userRepository.save(user1);
        
        Cliente cliente1 = new Cliente();
        cliente1.setUser(savedUser1);
        cliente1.setNome("João Silva");
        cliente1.setEmail("joao.silva@email.com");
        cliente1.setTelefone("11987654321");
        Cliente savedCliente1 = clienteRepository.save(cliente1);
        
        // Criar endereço para cliente1
        EnderecoRequestDTO enderecoDTO1 = new EnderecoRequestDTO();
        enderecoDTO1.setLogradouro("Rua das Flores");
        enderecoDTO1.setNumero("123");
        enderecoDTO1.setBairro("Centro");
        enderecoDTO1.setCidade("São Paulo");
        enderecoDTO1.setEstado("SP");
        enderecoDTO1.setCep("01310100");
        enderecoDTO1.setPrincipal(true);
        enderecoService.criarEndereco(enderecoDTO1, savedCliente1);
        aguardarRateLimit();

        // Cliente 2
        User user2 = new User();
        user2.setUsername("maria.santos@email.com");
        user2.setPassword(passwordEncoder.encode("123456"));
        
        Set<Role> roles2 = new HashSet<>();
        roles2.add(clienteRole);
        user2.setRoles(roles2);
        
        User savedUser2 = userRepository.save(user2);
        
        Cliente cliente2 = new Cliente();
        cliente2.setUser(savedUser2);
        cliente2.setNome("Maria Santos");
        cliente2.setEmail("maria.santos@email.com");
        cliente2.setTelefone("11912345678");
        Cliente savedCliente2 = clienteRepository.save(cliente2);
        
        // Criar endereço para cliente2
        EnderecoRequestDTO enderecoDTO2 = new EnderecoRequestDTO();
        enderecoDTO2.setLogradouro("Av. Paulista");
        enderecoDTO2.setNumero("456");
        enderecoDTO2.setBairro("Bela Vista");
        enderecoDTO2.setCidade("São Paulo");
        enderecoDTO2.setEstado("SP");
        enderecoDTO2.setCep("01310200");
        enderecoDTO2.setPrincipal(true);
        enderecoService.criarEndereco(enderecoDTO2, savedCliente2);
        aguardarRateLimit();

        // Cliente 3
        User user3 = new User();
        user3.setUsername("pedro.oliveira@email.com");
        user3.setPassword(passwordEncoder.encode("123456"));
        
        Set<Role> roles3 = new HashSet<>();
        roles3.add(clienteRole);
        user3.setRoles(roles3);
        
        User savedUser3 = userRepository.save(user3);
        
        Cliente cliente3 = new Cliente();
        cliente3.setUser(savedUser3);
        cliente3.setNome("Pedro Oliveira");
        cliente3.setEmail("pedro.oliveira@email.com");
        cliente3.setTelefone("11955554444");
        Cliente savedCliente3 = clienteRepository.save(cliente3);
        
        // Criar endereço para cliente3
        EnderecoRequestDTO enderecoDTO3 = new EnderecoRequestDTO();
        enderecoDTO3.setLogradouro("Rua Augusta");
        enderecoDTO3.setNumero("789");
        enderecoDTO3.setBairro("Consolação");
        enderecoDTO3.setCidade("São Paulo");
        enderecoDTO3.setEstado("SP");
        enderecoDTO3.setCep("01305100");
        enderecoDTO3.setPrincipal(true);
        enderecoService.criarEndereco(enderecoDTO3, savedCliente3);

        System.out.println("✅ Clientes de exemplo criados com sucesso!");
        System.out.println("   Cliente 1: João Silva (ID: 1) - Email: joao.silva@email.com - Senha: 123456");
        System.out.println("   Cliente 2: Maria Santos (ID: 2) - Email: maria.santos@email.com - Senha: 123456");
        System.out.println("   Cliente 3: Pedro Oliveira (ID: 3) - Email: pedro.oliveira@email.com - Senha: 123456");
    }

    @Transactional
    private void createTestRestaurantAndData() {
        // Criar usuário do restaurante
        User restauranteUser = new User();
        restauranteUser.setUsername("restaurante@teste.com");
        restauranteUser.setPassword(passwordEncoder.encode("123456"));
        
        Set<Role> restauranteRoles = new HashSet<>();
        Role restauranteRole = roleRepository.findByRoleName(ERole.ROLE_RESTAURANTE)
                .orElseThrow(() -> new RuntimeException("Role RESTAURANTE não encontrado"));
        restauranteRoles.add(restauranteRole);
        restauranteUser.setRoles(restauranteRoles);
        
        User savedRestauranteUser = userRepository.save(restauranteUser);

        // Criar restaurante aprovado
        Restaurante restaurante = new Restaurante();
        restaurante.setNome("Restaurante Teste");
        restaurante.setEmail("restaurante@teste.com");
        restaurante.setTelefone("(11) 99999-9999");
        restaurante.setStatus(StatusRestaurante.APPROVED);
        restaurante.setUser(savedRestauranteUser);
        Restaurante savedRestaurante = restauranteRepository.save(restaurante);
        
        // Criar endereço para restaurante
        EnderecoRequestDTO enderecoRestauranteDTO = new EnderecoRequestDTO();
        enderecoRestauranteDTO.setLogradouro("Rua Teste");
        enderecoRestauranteDTO.setNumero("123");
        enderecoRestauranteDTO.setBairro("Centro");
        enderecoRestauranteDTO.setCidade("São Paulo");
        enderecoRestauranteDTO.setEstado("SP");
        enderecoRestauranteDTO.setCep("01310100");
        enderecoRestauranteDTO.setPrincipal(true);
        enderecoService.criarEndereco(enderecoRestauranteDTO, savedRestaurante);
        aguardarRateLimit();
        
        restaurante = savedRestaurante;

        // Criar prato
        Prato prato = new Prato();
        prato.setNome("Hambúrguer Artesanal");
        prato.setDescricao("Hambúrguer com carne artesanal, queijo, alface e tomate");
        prato.setPreco(new BigDecimal("25.90"));
        prato.setCategoria(CategoriaMenu.MAIN);
        prato.setDisponivel(true);
        prato.setRestaurante(restaurante);
        prato = pratoRepository.save(prato);

        // Buscar primeiro cliente
        Cliente cliente = clienteRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Nenhum cliente encontrado"));

        // Criar carrinho com item
        Carrinho carrinho = new Carrinho();
        carrinho.setCliente(cliente);
        carrinho.setSubtotal(new BigDecimal("25.90"));
        carrinho.setTotal(new BigDecimal("25.90"));
        carrinho = carrinhoRepository.save(carrinho);

        CarrinhoItem item = new CarrinhoItem();
        item.setCarrinho(carrinho);
        item.setPrato(prato);
        item.setQuantidade(1);
        item.setPrecoUnitario(prato.getPreco());
        item.setSubtotal(prato.getPreco());
        carrinho.getItens().add(item);
        carrinhoRepository.save(carrinho);

        // Criar pedido com CREDIT_CARD
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setStatus(StatusPedido.CREATED);
        pedido.setMetodoPagamento(MetodoPagamento.CREDIT_CARD);
        // Buscar endereço principal do cliente usando o serviço
        Endereco enderecoEntrega = enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente não possui endereço cadastrado"));
        pedido.setEnderecoEntrega(enderecoEntrega);
        pedido.setSubtotal(new BigDecimal("25.90"));
        pedido.setTaxaEntrega(new BigDecimal("5.00"));
        pedido.setTotal(new BigDecimal("30.90"));
        pedido = pedidoRepository.save(pedido);

        PedidoItem pedidoItem = new PedidoItem();
        pedidoItem.setPedido(pedido);
        pedidoItem.setPrato(prato);
        pedidoItem.setQuantidade(1);
        pedidoItem.setPrecoUnitario(prato.getPreco());
        pedidoItem.setSubtotal(prato.getPreco());
        pedido.getItens().add(pedidoItem);
        pedidoRepository.save(pedido);

        System.out.println("✅ Dados de teste criados com sucesso!");
        System.out.println("   Restaurante: " + restaurante.getNome() + " (ID: " + restaurante.getId() + ") - Status: APPROVED");
        System.out.println("   Prato: " + prato.getNome() + " (ID: " + prato.getId() + ") - Preço: R$ " + prato.getPreco());
        System.out.println("   Carrinho: ID " + carrinho.getId() + " - Cliente: " + cliente.getNome());
        System.out.println("   Pedido: ID " + pedido.getId() + " - Método: CREDIT_CARD - Total: R$ " + pedido.getTotal());
        System.out.println("   Use o pedido ID " + pedido.getId() + " para testar pagamento com cartão!");
    }

    @Transactional
    private void createTestEntregador() {
        // Criar usuário do entregador
        User entregadorUser = new User();
        entregadorUser.setUsername("entregador@teste.com");
        entregadorUser.setPassword(passwordEncoder.encode("123456"));
        
        Set<Role> entregadorRoles = new HashSet<>();
        Role entregadorRole = roleRepository.findByRoleName(ERole.ROLE_ENTREGADOR)
                .orElseThrow(() -> new RuntimeException("Role ENTREGADOR não encontrado"));
        entregadorRoles.add(entregadorRole);
        entregadorUser.setRoles(entregadorRoles);
        
        User savedEntregadorUser = userRepository.save(entregadorUser);

        // Criar entregador aprovado
        Entregador entregador = new Entregador();
        entregador.setUser(savedEntregadorUser);
        entregador.setNome("Carlos Motoboy");
        entregador.setEmail("entregador@teste.com");
        entregador.setCpf("12345678900");
        entregador.setTelefone("(11) 98888-8888");
        entregador.setTipoVeiculo(TipoVeiculo.MOTO);
        entregador.setPlacaVeiculo("ABC-1234");
        entregador.setStatus(StatusEntregador.APPROVED);
        entregador.setDisponibilidade(DisponibilidadeEntregador.UNAVAILABLE);
        entregador.setLatitude(new BigDecimal("-23.550520"));
        entregador.setLongitude(new BigDecimal("-46.633308"));
        
        Entregador savedEntregador = entregadorRepository.save(entregador);

        System.out.println("✅ Entregador de teste criado com sucesso!");
        System.out.println("   Entregador: " + entregador.getNome() + " (ID: " + savedEntregador.getId() + ")");
        System.out.println("   Email: " + entregador.getEmail() + " - Senha: 123456");
        System.out.println("   Status: APPROVED - Veículo: " + entregador.getTipoVeiculo() + " - Placa: " + entregador.getPlacaVeiculo());
    }
    
    private void aguardarRateLimit() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ Interrupção durante espera do rate limit");
        }
    }

    @Transactional
    private void createInitialTaxas() {
        
        boolean taxaRestauranteExiste = configuracaoTaxaRepository
                .findByTipoTaxaAndAtivoTrue(TipoTaxa.TAXA_RESTAURANTE)
                .isPresent();
        boolean taxaEntregadorExiste = configuracaoTaxaRepository
                .findByTipoTaxaAndAtivoTrue(TipoTaxa.TAXA_ENTREGADOR)
                .isPresent();

       
        if (!taxaRestauranteExiste) {
            ConfiguracaoTaxa taxaRestaurante = new ConfiguracaoTaxa();
            taxaRestaurante.setTipoTaxa(TipoTaxa.TAXA_RESTAURANTE);
            taxaRestaurante.setPercentual(new BigDecimal("10.00"));
            taxaRestaurante.setAtivo(true);
            configuracaoTaxaRepository.save(taxaRestaurante);
            System.out.println("✅ Taxa de restaurante criada: 10.00%");
        }

     
        if (!taxaEntregadorExiste) {
            ConfiguracaoTaxa taxaEntregador = new ConfiguracaoTaxa();
            taxaEntregador.setTipoTaxa(TipoTaxa.TAXA_ENTREGADOR);
            taxaEntregador.setPercentual(new BigDecimal("15.00"));
            taxaEntregador.setAtivo(true);
            configuracaoTaxaRepository.save(taxaEntregador);
            System.out.println("✅ Taxa de entregador criada: 15.00%");
        }

        if (taxaRestauranteExiste && taxaEntregadorExiste) {
            System.out.println("✅ Taxas já existem no sistema");
        }
    }
}