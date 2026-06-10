package com.siseg.config;

import com.siseg.model.Avaliacao;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.PedidoItem;
import com.siseg.model.Prato;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.MetodoPagamento;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.model.enumerations.StatusRestaurante;
import com.siseg.repository.AvaliacaoRepository;
import com.siseg.repository.ClienteRepository;
import com.siseg.repository.EntregadorRepository;
import com.siseg.repository.PedidoRepository;
import com.siseg.repository.PratoRepository;
import com.siseg.repository.RestauranteRepository;
import com.siseg.service.EnderecoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Cria pedidos entregues e avaliações para restaurantes demo, para exibir notas na listagem.
 */
@Component
@Profile("!test")
@Order(3)
public class DemoAvaliacoesSeeder implements CommandLineRunner {

    private static final String SEED_PEDIDO_MARKER = "[demo-seed-avaliacao]";

    private static final String[] COMENTARIOS = {
            "Entrega rápida e comida deliciosa!",
            "Muito bom, pedirei de novo.",
            "Porção generosa e bem temperado.",
            "Atendimento excelente.",
            "Sabor caseiro, adorei.",
            "Chegou quentinho e no prazo.",
            "Qualidade consistente.",
            "Ótimo custo-benefício.",
            "Cardápio variado e bem apresentado.",
            "Experiência muito positiva.",
    };

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private EntregadorRepository entregadorRepository;

    @Autowired
    private EnderecoService enderecoService;

    @Override
    @Transactional
    public void run(String... args) {
        List<Cliente> clientes = clienteRepository.findAll();
        if (clientes.isEmpty()) {
            return;
        }

        Optional<Entregador> entregadorOpt = entregadorRepository.findByEmail("entregador@teste.com");

        List<Restaurante> alvos = restauranteRepository.findAll().stream()
                .filter(r -> r.getStatus() == StatusRestaurante.APPROVED)
                .filter(r -> {
                    String email = r.getEmail();
                    return email != null
                            && (email.endsWith("@ifeats.com") || "restaurante@teste.com".equals(email));
                })
                .toList();

        if (alvos.isEmpty()) {
            return;
        }

        int criadas = 0;
        int clienteIdx = 0;

        for (Restaurante restaurante : alvos) {
            int meta = 6 + (int) (restaurante.getId() % 7);
            long atual = avaliacaoRepository.countByRestauranteId(restaurante.getId());
            int faltam = (int) Math.max(0, meta - atual);
            if (faltam == 0) {
                continue;
            }

            Prato prato = pratoRepository.findByRestauranteId(restaurante.getId(), Pageable.ofSize(1))
                    .getContent()
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (prato == null) {
                continue;
            }

            for (int i = 0; i < faltam; i++) {
                Cliente cliente = clientes.get(clienteIdx % clientes.size());
                clienteIdx++;

                Optional<Endereco> enderecoCliente = enderecoService.buscarEnderecoPrincipalCliente(cliente.getId());
                if (enderecoCliente.isEmpty()) {
                    continue;
                }

                Pedido pedido = criarPedidoEntregue(
                        cliente,
                        restaurante,
                        prato,
                        enderecoCliente.get(),
                        entregadorOpt.orElse(null),
                        i
                );
                pedido = pedidoRepository.save(pedido);

                Avaliacao avaliacao = new Avaliacao();
                avaliacao.setPedido(pedido);
                avaliacao.setCliente(cliente);
                avaliacao.setRestaurante(restaurante);
                entregadorOpt.ifPresent(avaliacao::setEntregador);

                final int indice = i;
                int nota = notaDemo(restaurante.getId(), indice);
                avaliacao.setNotaRestaurante(nota);
                avaliacao.setNotaPedido(nota);
                entregadorOpt.ifPresent(e -> avaliacao.setNotaEntregador(Math.max(4, nota - (indice % 2))));
                avaliacao.setComentarioRestaurante(
                        COMENTARIOS[(int) ((restaurante.getId() + i) % COMENTARIOS.length)]
                );

                avaliacaoRepository.save(avaliacao);
                criadas++;
            }
        }

        if (criadas > 0) {
            System.out.println("✅ Avaliações demo: " + criadas + " criadas para restaurantes @ifeats.com.");
        }
    }

    private Pedido criarPedidoEntregue(
            Cliente cliente,
            Restaurante restaurante,
            Prato prato,
            Endereco enderecoEntrega,
            Entregador entregador,
            int indice
    ) {
        BigDecimal subtotal = prato.getPreco();
        BigDecimal taxaEntrega = new BigDecimal("5.00");
        BigDecimal total = subtotal.add(taxaEntrega);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setStatus(StatusPedido.DELIVERED);
        pedido.setMetodoPagamento(MetodoPagamento.PIX);
        pedido.setEnderecoEntrega(enderecoEntrega);
        pedido.setSubtotal(subtotal);
        pedido.setTaxaEntrega(taxaEntrega);
        pedido.setTotal(total);
        pedido.setObservacoes(SEED_PEDIDO_MARKER);
        if (entregador != null) {
            pedido.setEntregador(entregador);
        }

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setPrato(prato);
        item.setQuantidade(1);
        item.setPrecoUnitario(prato.getPreco());
        item.setSubtotal(prato.getPreco());
        pedido.getItens().add(item);

        return pedido;
    }

    private int notaDemo(long restauranteId, int indice) {
        int base = 4 + (int) (restauranteId % 2);
        int variacao = indice % 3;
        return Math.min(5, base + (variacao == 2 ? 0 : variacao == 0 ? 1 : 0));
    }
}
