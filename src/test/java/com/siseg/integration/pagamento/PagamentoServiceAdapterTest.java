package com.siseg.integration.pagamento;

import com.siseg.dto.pagamento.CartaoCreditoRequestDTO;
import com.siseg.dto.pagamento.ClienteInfoDTO;
import com.siseg.dto.pagamento.CriarPagamentoCompletoRequestDTO;
import com.siseg.dto.pagamento.CriarPagamentoRequestDTO;
import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.User;
import com.siseg.model.enumerations.MetodoPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PagamentoServiceAdapterTest {

    private PagamentoServiceAdapter adapter;
    private Pedido pedido;
    private Cliente cliente;
    private User user;

    @BeforeEach
    void setUp() {
        adapter = new PagamentoServiceAdapter();

        user = new User();
        user.setId(1L);
        user.setUsername("cliente@teste.com");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente@teste.com");
        cliente.setTelefone("11999999999");
        cliente.setUser(user);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setCliente(cliente);
        pedido.setMetodoPagamento(MetodoPagamento.PIX);
        pedido.setTotal(new BigDecimal("100.00"));
        pedido.setTroco(null);
    }

    @Test
    void deveAdaptarPedidoParaCriacaoPagamentoSemCartao() {
        CriarPagamentoCompletoRequestDTO result = adapter.adaptarPedidoParaCriacaoPagamento(pedido, null);

        assertNotNull(result);
        assertNotNull(result.getPagamento());
        assertNotNull(result.getCliente());

        CriarPagamentoRequestDTO pagamentoRequest = result.getPagamento();
        assertEquals(1L, pagamentoRequest.getPedidoId());
        assertEquals(MetodoPagamento.PIX, pagamentoRequest.getMetodoPagamento());
        assertEquals(new BigDecimal("100.00"), pagamentoRequest.getValor());
        assertNull(pagamentoRequest.getCartaoCredito());

        ClienteInfoDTO clienteInfo = result.getCliente();
        assertEquals(1L, clienteInfo.getId());
        assertEquals("Cliente Teste", clienteInfo.getNome());
        assertEquals("cliente@teste.com", clienteInfo.getEmail());
        assertEquals("11999999999", clienteInfo.getTelefone());
    }

    @Test
    void deveAdaptarPedidoParaCriacaoPagamentoComCartao() {
        CartaoCreditoRequestDTO cartaoDTO = new CartaoCreditoRequestDTO();
        cartaoDTO.setNumero("4111111111111111");
        cartaoDTO.setNomeTitular("Cliente Teste");
        cartaoDTO.setValidade("12/25");
        cartaoDTO.setCvv("123");

        pedido.setMetodoPagamento(MetodoPagamento.CREDIT_CARD);

        CriarPagamentoCompletoRequestDTO result = adapter.adaptarPedidoParaCriacaoPagamento(pedido, cartaoDTO);

        assertNotNull(result);
        assertNotNull(result.getPagamento());
        assertEquals(MetodoPagamento.CREDIT_CARD, result.getPagamento().getMetodoPagamento());
        assertNotNull(result.getPagamento().getCartaoCredito());
        assertEquals("4111111111111111", result.getPagamento().getCartaoCredito().getNumero());
    }

    @Test
    void deveAdaptarClienteParaDTO() {
        CriarPagamentoCompletoRequestDTO result = adapter.adaptarPedidoParaCriacaoPagamento(pedido, null);

        ClienteInfoDTO clienteInfo = result.getCliente();
        assertNotNull(clienteInfo);
        assertEquals(cliente.getId(), clienteInfo.getId());
        assertEquals(cliente.getNome(), clienteInfo.getNome());
        assertEquals(cliente.getEmail(), clienteInfo.getEmail());
        assertEquals(cliente.getTelefone(), clienteInfo.getTelefone());
        assertNotNull(clienteInfo.getCpfCnpj());
    }
}

