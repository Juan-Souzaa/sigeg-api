package com.siseg.config;

import com.siseg.model.Endereco;
import com.siseg.model.Prato;
import com.siseg.model.Restaurante;
import com.siseg.model.Role;
import com.siseg.model.User;
import com.siseg.model.enumerations.CategoriaMenu;
import com.siseg.model.enumerations.ERole;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.model.enumerations.TipoEndereco;
import com.siseg.repository.EnderecoRepository;
import com.siseg.repository.PratoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.repository.RoleRepository;
import com.siseg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Popula o catálogo com restaurantes e pratos de demonstração (com fotos).
 * Cada restaurante é identificado pelo e-mail: novos entram no catálogo a cada deploy.
 */
@Component
@Profile("!test")
@Order(2)
public class DemoCatalogSeeder implements CommandLineRunner {

    private static final String SEED_MARKER_EMAIL = "demo.bella.pizza@ifeats.com";
    private static final String DEMO_PASSWORD = "123456";
    private static final String CIDADE_DEMO = "Guarulhos";
    private static final String ESTADO_DEMO = "SP";

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        enrichRestauranteTeste();

        Role restauranteRole = roleRepository.findByRoleName(ERole.ROLE_RESTAURANTE)
                .orElseThrow(() -> new RuntimeException("Role RESTAURANTE não encontrado"));

        List<DemoRestaurante> catalogo = buildCatalogo();
        int novos = 0;
        int pratosNovos = 0;

        for (DemoRestaurante demo : catalogo) {
            var existente = restauranteRepository.findByEmail(demo.email());
            if (existente.isPresent()) {
                Restaurante restaurante = existente.get();
                aplicarEnderecoRestaurante(
                        restaurante,
                        demo.logradouro(),
                        demo.numero(),
                        demo.bairro(),
                        demo.cep(),
                        demo.latitude(),
                        demo.longitude()
                );
                sincronizarFotoRestaurante(restaurante, demo.fotoUrl());
                pratosNovos += sincronizarCardapio(restaurante, demo.pratos());
            } else {
                criarRestauranteComCardapio(demo, restauranteRole);
                novos++;
            }
        }

        if (novos > 0 || pratosNovos > 0) {
            System.out.println("✅ Catálogo demo atualizado: " + novos + " restaurantes novos, "
                    + pratosNovos + " pratos adicionados.");
            System.out.println("   Total no catálogo: " + catalogo.size() + " restaurantes demo em Guarulhos.");
            System.out.println("   Login: e-mail do restaurante / senha: " + DEMO_PASSWORD);
        }
    }

    private void enrichRestauranteTeste() {
        restauranteRepository.findByEmail("restaurante@teste.com").ifPresent(restaurante -> {
            boolean alterado = false;

            if (restaurante.getFotoUrl() == null || restaurante.getFotoUrl().isBlank()) {
                restaurante.setFotoUrl("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&q=80");
                alterado = true;
            }

            if (restaurante.getRaioEntregaKm() == null) {
                restaurante.setRaioEntregaKm(new BigDecimal("10.00"));
                alterado = true;
            }

            if (alterado) {
                restauranteRepository.save(restaurante);
            }

            pratoRepository.findByRestauranteId(restaurante.getId(), Pageable.unpaged()).forEach(prato -> {
                if (prato.getFotoUrl() == null || prato.getFotoUrl().isBlank()) {
                    String nomeLower = prato.getNome().toLowerCase();
                    String foto = (nomeLower.contains("hambúrguer") || nomeLower.contains("hamburger"))
                            ? "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=800&q=80"
                            : "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80";
                    prato.setFotoUrl(foto);
                    pratoRepository.save(prato);
                }
            });

            criarPratoSeNaoExiste(restaurante, "Combo Família",
                    "2 burgers + 2 batatas + 2 refrigerantes",
                    new BigDecimal("69.90"), CategoriaMenu.MAIN,
                    "https://images.unsplash.com/photo-1550547660-d9450f859349?w=800&q=80");
            criarPratoSeNaoExiste(restaurante, "Milkshake de Morango",
                    "Cremoso com frutas frescas",
                    new BigDecimal("16.90"), CategoriaMenu.DRINK,
                    "https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=800&q=80");

            long pratos = pratoRepository.findByRestauranteId(restaurante.getId(), Pageable.unpaged()).getTotalElements();
            if (pratos < 4) {
                criarPratoSeNaoExiste(restaurante, "Hambúrguer Artesanal",
                        "Blend 180g, queijo cheddar, alface, tomate e molho da casa",
                        new BigDecimal("25.90"), CategoriaMenu.MAIN,
                        "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=800&q=80");
                criarPratoSeNaoExiste(restaurante, "Batata Frita Crocante",
                        "Porção generosa com tempero especial",
                        new BigDecimal("14.90"), CategoriaMenu.STARTER,
                        "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=800&q=80");
                criarPratoSeNaoExiste(restaurante, "Refrigerante Lata",
                        "Coca-Cola, Guaraná ou Sprite 350ml",
                        new BigDecimal("6.90"), CategoriaMenu.DRINK,
                        "https://images.unsplash.com/photo-1629203851122-3726ecdf080e?w=800&q=80");
                criarPratoSeNaoExiste(restaurante, "Brownie com Sorvete",
                        "Brownie quentinho com bola de sorvete de creme",
                        new BigDecimal("18.90"), CategoriaMenu.DESSERT,
                        "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=800&q=80");
                System.out.println("✅ Restaurante Teste enriquecido com fotos e cardápio extra.");
            }

            aplicarEnderecoRestaurante(
                    restaurante,
                    "Rua Anton Phillips",
                    "100",
                    "Centro",
                    "07013000",
                    new BigDecimal("-23.46100000"),
                    new BigDecimal("-46.53100000")
            );
        });
    }

    private int sincronizarCardapio(Restaurante restaurante, List<DemoPrato> pratos) {
        int adicionados = 0;
        for (DemoPrato item : pratos) {
            var existentes = pratoRepository.findByRestauranteId(restaurante.getId(), Pageable.unpaged());
            var pratoOpt = existentes.stream()
                    .filter(p -> item.nome().equalsIgnoreCase(p.getNome()))
                    .findFirst();

            if (pratoOpt.isPresent()) {
                Prato prato = pratoOpt.get();
                if (prato.getFotoUrl() == null || !item.fotoUrl().equals(prato.getFotoUrl())) {
                    prato.setFotoUrl(item.fotoUrl());
                    pratoRepository.save(prato);
                }
                continue;
            }

            criarPratoSeNaoExiste(
                    restaurante,
                    item.nome(),
                    item.descricao(),
                    item.preco(),
                    item.categoria(),
                    item.fotoUrl()
            );
            adicionados++;
        }
        return adicionados;
    }

    private void sincronizarFotoRestaurante(Restaurante restaurante, String fotoUrl) {
        if (fotoUrl != null && !fotoUrl.equals(restaurante.getFotoUrl())) {
            restaurante.setFotoUrl(fotoUrl);
            restauranteRepository.save(restaurante);
        }
    }

    private void aplicarEnderecoRestaurante(
            Restaurante restaurante,
            String logradouro,
            String numero,
            String bairro,
            String cep,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        Endereco endereco = enderecoRepository
                .findByRestauranteIdAndPrincipal(restaurante.getId(), true)
                .orElseGet(() -> {
                    Endereco novo = new Endereco();
                    novo.setRestaurante(restaurante);
                    novo.setTipo(TipoEndereco.OUTRO);
                    novo.setPrincipal(true);
                    return novo;
                });

        endereco.setLogradouro(logradouro);
        endereco.setNumero(numero);
        endereco.setBairro(bairro);
        endereco.setCidade(CIDADE_DEMO);
        endereco.setEstado(ESTADO_DEMO);
        endereco.setCep(cep);
        endereco.setLatitude(latitude);
        endereco.setLongitude(longitude);
        enderecoRepository.save(endereco);
    }

    private void criarRestauranteComCardapio(DemoRestaurante demo, Role restauranteRole) {
        User user = new User();
        user.setUsername(demo.email());
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        Set<Role> roles = new HashSet<>();
        roles.add(restauranteRole);
        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        Restaurante restaurante = new Restaurante();
        restaurante.setNome(demo.nome());
        restaurante.setEmail(demo.email());
        restaurante.setTelefone(demo.telefone());
        restaurante.setStatus(StatusRestaurante.APPROVED);
        restaurante.setAtivo(true);
        restaurante.setRaioEntregaKm(demo.raioEntregaKm());
        restaurante.setFotoUrl(demo.fotoUrl());
        restaurante.setUser(savedUser);
        Restaurante savedRestaurante = restauranteRepository.save(restaurante);

        Endereco endereco = new Endereco();
        endereco.setRestaurante(savedRestaurante);
        endereco.setLogradouro(demo.logradouro());
        endereco.setNumero(demo.numero());
        endereco.setBairro(demo.bairro());
        endereco.setCidade(CIDADE_DEMO);
        endereco.setEstado(ESTADO_DEMO);
        endereco.setCep(demo.cep());
        endereco.setLatitude(demo.latitude());
        endereco.setLongitude(demo.longitude());
        endereco.setTipo(TipoEndereco.OUTRO);
        endereco.setPrincipal(true);
        enderecoRepository.save(endereco);

        for (DemoPrato item : demo.pratos()) {
            Prato prato = new Prato();
            prato.setNome(item.nome());
            prato.setDescricao(item.descricao());
            prato.setPreco(item.preco());
            prato.setCategoria(item.categoria());
            prato.setDisponivel(true);
            prato.setFotoUrl(item.fotoUrl());
            prato.setRestaurante(savedRestaurante);
            pratoRepository.save(prato);
        }
    }

    private void criarPratoSeNaoExiste(
            Restaurante restaurante,
            String nome,
            String descricao,
            BigDecimal preco,
            CategoriaMenu categoria,
            String fotoUrl
    ) {
        boolean existe = pratoRepository.findByRestauranteId(restaurante.getId(), Pageable.unpaged()).stream()
                .anyMatch(p -> nome.equalsIgnoreCase(p.getNome()));
        if (existe) {
            return;
        }

        Prato prato = new Prato();
        prato.setNome(nome);
        prato.setDescricao(descricao);
        prato.setPreco(preco);
        prato.setCategoria(categoria);
        prato.setDisponivel(true);
        prato.setFotoUrl(fotoUrl);
        prato.setRestaurante(restaurante);
        pratoRepository.save(prato);
    }

    private List<DemoRestaurante> buildCatalogo() {
        return List.of(
                new DemoRestaurante(
                        "Bella Pizza House",
                        SEED_MARKER_EMAIL,
                        "(11) 3456-1001",
                        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&q=80",
                        new BigDecimal("12.00"),
                        "Av. Guarulhos", "1500", "Centro", "07010000",
                        new BigDecimal("-23.46250000"), new BigDecimal("-46.53380000"),
                        List.of(
                                prato("Pizza Margherita", "Molho de tomate, mussarela e manjericão fresco", "42.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=800&q=80"),
                                prato("Pizza Calabresa", "Calabresa fatiada, cebola e azeitonas", "44.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1628840042765-356cda07504e?w=800&q=80"),
                                prato("Bruschetta Italiana", "Pão italiano com tomate, manjericão e azeite", "22.90", CategoriaMenu.STARTER,
                                        DemoImageUrls.GARLIC_BREAD),
                                prato("Tiramisu", "Clássico italiano com café e mascarpone", "19.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=800&q=80"),
                                prato("Suco de Limonada Siciliana", "Limões frescos com hortelã", "12.90", CategoriaMenu.DRINK,
                                        "https://images.unsplash.com/photo-1621263764928-df1444c5e859?w=800&q=80"),
                                prato("Pizza Quatro Queijos", "Mussarela, gorgonzola, parmesão e provolone", "46.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800&q=80"),
                                prato("Lasanha à Bolonhesa", "Massa fresca com ragú e queijo gratinado", "38.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1574894709920-11b28e7367e3?w=800&q=80"),
                                prato("Garlic Bread", "Pão com manteiga de alho e ervas", "16.90", CategoriaMenu.STARTER,
                                        "https://images.unsplash.com/photo-1619535860434-ba1d8fa12536?w=800&q=80")
                        )
                ),
                new DemoRestaurante(
                        "Burger Lab GRU",
                        "demo.burger.lab@ifeats.com",
                        "(11) 3456-1002",
                        "https://images.unsplash.com/photo-1550547660-d9450f859349?w=800&q=80",
                        new BigDecimal("10.00"),
                        "Rua Dom Pedro II", "220", "Jardim Maia", "07111000",
                        new BigDecimal("-23.45120000"), new BigDecimal("-46.51850000"),
                        List.of(
                                prato("Smash Burger Duplo", "Dois blends 90g, queijo americano e pickles", "34.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1550547660-d9450f859349?w=800&q=80"),
                                prato("Chicken Crispy Burger", "Frango empanado crocante com maionese de alho", "29.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=800&q=80"),
                                prato("Onion Rings", "Anéis de cebola empanados", "16.90", CategoriaMenu.STARTER,
                                        DemoImageUrls.ONION_RINGS),
                                prato("Milkshake de Chocolate", "Cremoso com calda belga", "17.90", CategoriaMenu.DRINK,
                                        "https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=800&q=80"),
                                prato("Bacon Supreme Burger", "Blend 180g, bacon crocante e cheddar", "36.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=800&q=80"),
                                prato("Nuggets de Frango", "8 unidades com molho barbecue", "18.90", CategoriaMenu.STARTER,
                                        DemoImageUrls.NUGGETS),
                                prato("Suco Natural de Laranja", "Espremido na hora, 400ml", "11.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.SUCO)
                        )
                ),
                new DemoRestaurante(
                        "Sushi Zen",
                        "demo.sushi.zen@ifeats.com",
                        "(11) 3456-1003",
                        DemoImageUrls.SUSHI_REST,
                        new BigDecimal("8.00"),
                        "Rua Eduardo Silva", "310", "Macedo", "07115000",
                        new BigDecimal("-23.46810000"), new BigDecimal("-46.55180000"),
                        List.of(
                                prato("Combo Salmão 20 peças", "Sashimi, nigiri e uramaki de salmão", "59.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=800&q=80"),
                                prato("Hot Roll Filadélfia", "8 peças com cream cheese e salmão", "32.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.SUSHI_ROLL),
                                prato("Edamame com Flor de Sal", "Soja verde no vapor", "14.90", CategoriaMenu.STARTER,
                                        "https://images.unsplash.com/photo-1617093727343-374698b1b08d?w=800&q=80"),
                                prato("Chá Gelado de Pêssego", "Refrescante, 500ml", "9.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.CHA),
                                prato("Temaki de Camarão", "Arroz, camarão empanado e cream cheese", "24.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.SUSHI_ROLL),
                                prato("Yakissoba de Legumes", "Macarrão, legumes e molho agridoce", "34.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.WOK),
                                prato("Mochi de Morango", "Doce japonês recheado, 2 unidades", "14.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=800&q=80")
                        )
                ),
                new DemoRestaurante(
                        "Green Bowl",
                        "demo.green.bowl@ifeats.com",
                        "(11) 3456-1004",
                        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800&q=80",
                        new BigDecimal("9.00"),
                        "Rua Dr. João Júlio", "980", "Vila Galvão", "07042000",
                        new BigDecimal("-23.47850000"), new BigDecimal("-46.54520000"),
                        List.of(
                                prato("Bowl Mediterrâneo", "Quinoa, falafel, homus e legumes grelhados", "36.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80"),
                                prato("Salada Caesar com Frango", "Alface romana, croutons e parmesão", "28.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1546793665-c74683f339c1?w=800&q=80"),
                                prato("Wrap de Atum", "Atum, cream cheese light e rúcula", "26.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=800&q=80"),
                                prato("Suco Detox Verde", "Couve, maçã, gengibre e limão", "13.90", CategoriaMenu.DRINK,
                                        "https://images.unsplash.com/photo-1610970881699-44a5587cabec?w=800&q=80"),
                                prato("Bowl Proteico Fit", "Frango grelhado, arroz integral e brócolis", "39.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80"),
                                prato("Smoothie de Frutas Vermelhas", "Morango, mirtilo e banana", "15.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.VITAMINA),
                                prato("Chips de Batata Doce", "Assados com alecrim", "12.90", CategoriaMenu.STARTER,
                                        "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=800&q=80")
                        )
                ),
                new DemoRestaurante(
                        "Doce Mania",
                        "demo.doce.mania@ifeats.com",
                        "(11) 3456-1005",
                        "https://images.unsplash.com/photo-1551024506-0bccd828d307?w=800&q=80",
                        new BigDecimal("11.00"),
                        "Av. Tiradentes", "650", "Picanço", "07220000",
                        new BigDecimal("-23.44830000"), new BigDecimal("-46.51010000"),
                        List.of(
                                prato("Cheesecake de Frutas Vermelhas", "Base crocante com calda artesanal", "21.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1524351199678-941a58a3df50?w=800&q=80"),
                                prato("Croissant de Chocolate", "Massa folhada com recheio belga", "12.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=800&q=80"),
                                prato("Café Especial Coado", "Grãos selecionados, 200ml", "8.90", CategoriaMenu.DRINK,
                                        "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&q=80"),
                                prato("Torta de Limão", "Merengue italiano e raspas de limão", "17.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1519915028121-7d3463d20b13?w=800&q=80"),
                                prato("Waffle Belga", "Com sorvete e calda de chocolate", "22.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1562376552-0d160a2f238d?w=800&q=80"),
                                prato("Brigadeiro Gourmet", "Caixa com 6 unidades", "19.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=800&q=80"),
                                prato("Chá Latte", "Chai com leite vaporizado", "10.90", CategoriaMenu.DRINK,
                                        "https://images.unsplash.com/photo-1571934811356-5cc061b6821f?w=800&q=80")
                        )
                ),
                new DemoRestaurante(
                        "Tempero Brasileiro",
                        "demo.tempero.brasileiro@ifeats.com",
                        "(11) 3456-1006",
                        "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800&q=80",
                        new BigDecimal("10.00"),
                        "Rua José de Andrade", "85", "Cumbica", "07140000",
                        new BigDecimal("-23.43580000"), new BigDecimal("-46.47560000"),
                        List.of(
                                prato("Feijoada Completa", "Acompanha arroz, couve, farofa e laranja", "48.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1544025162-d76694265947?w=800&q=80"),
                                prato("Picanha na Brasa", "Com vinagrete e mandioca", "62.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=800&q=80"),
                                prato("Pastel de Queijo", "Massa crocante, 2 unidades", "14.90", CategoriaMenu.STARTER,
                                        "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=800&q=80"),
                                prato("Caipirinha de Limão", "Clássica brasileira", "16.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.CAIPIRINHA),
                                prato("Pudim de Leite", "Receita da vovó com calda caramelizada", "15.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1587314168485-3236d6710814?w=800&q=80"),
                                prato("Strogonoff de Filé", "Arroz branco e batata palha", "44.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.STROGONOFF),
                                prato("Moqueca de Peixe", "Com arroz e pirão", "52.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=800&q=80"),
                                prato("Suco de Acerola", "Natural, 400ml", "9.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.SUCO)
                        )
                ),
                new DemoRestaurante(
                        "Churrasco do Zé",
                        "demo.churrasco.ze@ifeats.com",
                        "(11) 3456-1007",
                        "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=800&q=80",
                        new BigDecimal("12.00"),
                        "Rua Silvestre Pires", "410", "Bosque Maia", "07110000",
                        new BigDecimal("-23.45480000"), new BigDecimal("-46.52560000"),
                        List.of(
                                prato("Picanha na Chapa", "300g com farofa e vinagrete", "68.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=800&q=80"),
                                prato("Costela Suína", "Assada lentamente, 400g", "54.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?w=800&q=80"),
                                prato("Linguiça Toscana", "Grelhada com pão de alho", "32.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=800&q=80"),
                                prato("Queijo Coalho", "Na brasa com melado", "24.90", CategoriaMenu.STARTER,
                                        "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=800&q=80"),
                                prato("Cerveja Artesanal", "IPA gelada, 473ml", "18.90", CategoriaMenu.DRINK,
                                        "https://images.unsplash.com/photo-1608270586620-248524c67de9?w=800&q=80")
                        )
                ),
                new DemoRestaurante(
                        "Padaria Bom Dia",
                        "demo.padaria.bomdia@ifeats.com",
                        "(11) 3456-1008",
                        "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=800&q=80",
                        new BigDecimal("10.00"),
                        "Av. Paulo Faccini", "780", "Parque Continental", "07077000",
                        new BigDecimal("-23.47020000"), new BigDecimal("-46.52840000"),
                        List.of(
                                prato("Pão na Chapa com Manteiga", "Pão francês crocante", "6.90", CategoriaMenu.STARTER,
                                        "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=800&q=80"),
                                prato("Misto Quente Premium", "Presunto e queijo com ovo", "14.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=800&q=80"),
                                prato("Coxinha de Frango", "Massa cremosa, 2 unidades", "11.90", CategoriaMenu.STARTER,
                                        "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=800&q=80"),
                                prato("Suco de Maracujá", "Natural, 300ml", "8.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.SUCO),
                                prato("Sonho de Creme", "Recheio de confeiteiro", "7.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=800&q=80"),
                                prato("Café com Leite Grande", "Tradicional da casa", "7.50", CategoriaMenu.DRINK,
                                        "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&q=80")
                        )
                ),
                new DemoRestaurante(
                        "Açaí da Praça",
                        "demo.acai.praca@ifeats.com",
                        "(11) 3456-1009",
                        DemoImageUrls.ACAI,
                        new BigDecimal("11.00"),
                        "Rua Campos Sales", "95", "Ponte Grande", "07031000",
                        new BigDecimal("-23.46550000"), new BigDecimal("-46.51980000"),
                        List.of(
                                prato("Açaí 500ml Tradicional", "Com banana e granola", "22.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.ACAI),
                                prato("Açaí 700ml Turbinado", "Morango, leite em pó e paçoca", "32.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.ACAI_TURBINADO),
                                prato("Bowl de Frutas", "Melancia, manga, uva e kiwi", "19.90", CategoriaMenu.DESSERT,
                                        DemoImageUrls.FRUTAS),
                                prato("Vitamina de Banana", "Com aveia e mel", "12.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.VITAMINA),
                                prato("Tapioca de Coco", "Recheio doce com leite condensado", "13.90", CategoriaMenu.DESSERT,
                                        DemoImageUrls.TAPIOCA_DOCE)
                        )
                ),
                new DemoRestaurante(
                        "China Wok Express",
                        "demo.china.wok@ifeats.com",
                        "(11) 3456-1010",
                        DemoImageUrls.WOK,
                        new BigDecimal("10.00"),
                        "Rua Agulhas Negras", "520", "Gopoúva", "07020000",
                        new BigDecimal("-23.45890000"), new BigDecimal("-46.53720000"),
                        List.of(
                                prato("Yakissoba de Carne", "Macarrão, legumes e molho especial", "36.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.WOK),
                                prato("Frango Xadrez", "Com pimentão e amendoim", "34.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=800&q=80"),
                                prato("Rolinho Primavera", "4 unidades crocantes", "16.90", CategoriaMenu.STARTER,
                                        DemoImageUrls.ROLINHO_PRIMAVERA),
                                prato("Arroz Chop Suey", "Legumes salteados no wok", "28.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.ARROZ),
                                prato("Chá de Lichia", "Gelado, 500ml", "9.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.CHA)
                        )
                ),
                new DemoRestaurante(
                        "Massas & Cia",
                        "demo.massas.cia@ifeats.com",
                        "(11) 3456-1011",
                        "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=800&q=80",
                        new BigDecimal("9.00"),
                        "Rua Floripes Calil", "180", "Vila Endres", "07043000",
                        new BigDecimal("-23.47680000"), new BigDecimal("-46.54150000"),
                        List.of(
                                prato("Espaguete à Carbonara", "Bacon, gema e parmesão", "39.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.ESPAGUETE),
                                prato("Penne ao Molho Pesto", "Manjericão, pinoli e azeite", "37.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=800&q=80"),
                                prato("Nhoque ao Sugo", "Batata com molho de tomate fresco", "35.90", CategoriaMenu.MAIN,
                                        "https://images.unsplash.com/photo-1551183053-bf91a1d81141?w=800&q=80"),
                                prato("Bruschetta Clássica", "Tomate, alho e azeite", "18.90", CategoriaMenu.STARTER,
                                        DemoImageUrls.GARLIC_BREAD),
                                prato("Tiramisu da Casa", "Sobremesa italiana", "16.90", CategoriaMenu.DESSERT,
                                        "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=800&q=80")
                        )
                ),
                new DemoRestaurante(
                        "Tapiocaria Nordeste",
                        "demo.tapioca.nordeste@ifeats.com",
                        "(11) 3456-1012",
                        DemoImageUrls.TAPIOCA,
                        new BigDecimal("10.00"),
                        "Rua Santa Francisca", "340", "Jardim Santa Francisca", "07143000",
                        new BigDecimal("-23.44960000"), new BigDecimal("-46.52230000"),
                        List.of(
                                prato("Tapioca de Carne de Sol", "Com queijo coalho e manteiga de garrafa", "28.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.TAPIOCA_CARNE_SOL),
                                prato("Tapioca de Frango com Catupiry", "Recheio cremoso", "24.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.TAPIOCA_FRANGO),
                                prato("Baião de Dois", "Arroz, feijão verde e queijo coalho", "31.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.BAIAO),
                                prato("Caldo de Cana", "Copo 500ml gelado", "8.90", CategoriaMenu.DRINK,
                                        DemoImageUrls.CALDO_CANA),
                                prato("Cartola", "Banana, queijo e canela", "16.90", CategoriaMenu.DESSERT,
                                        DemoImageUrls.CARTOLA),
                                prato("Cuscuz com Ovo e Queijo", "Café da manhã nordestino", "18.90", CategoriaMenu.MAIN,
                                        DemoImageUrls.CUSCUZ)
                        )
                )
        );
    }

    private static DemoPrato prato(String nome, String descricao, String preco, CategoriaMenu categoria, String fotoUrl) {
        return new DemoPrato(nome, descricao, new BigDecimal(preco), categoria, fotoUrl);
    }

    private record DemoRestaurante(
            String nome,
            String email,
            String telefone,
            String fotoUrl,
            BigDecimal raioEntregaKm,
            String logradouro,
            String numero,
            String bairro,
            String cep,
            BigDecimal latitude,
            BigDecimal longitude,
            List<DemoPrato> pratos
    ) {}

    private record DemoPrato(
            String nome,
            String descricao,
            BigDecimal preco,
            CategoriaMenu categoria,
            String fotoUrl
    ) {}
}
