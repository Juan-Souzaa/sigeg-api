package com.siseg.service.pedido;

import com.siseg.model.Cliente;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoNotificacaoServiceTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PedidoNotificacaoService pedidoNotificacaoService;

    private Pedido pedido;
    private Cliente cliente;
    private Restaurante restaurante;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente@test.com");
        cliente.setTelefone("11999999999");

        restaurante = new Restaurante();
        restaurante.setId(2L);
        restaurante.setNome("Restaurante Teste");
        restaurante.setEmail("restaurante@test.com");

        pedido = new Pedido();
        pedido.setId(100L);
        pedido.setCliente(cliente);
        pedido.setRestaurante(restaurante);
        pedido.setStatus(StatusPedido.CONFIRMED);
        pedido.setTotal(new BigDecimal("75.50"));
    }

    @Test
    void deveEnviarNotificacoesAoConfirmarPedido() {
        doNothing().when(notificationService).notifyRestaurantNewOrder(
                eq(pedido.getId()), eq(restaurante.getEmail()), eq(pedido.getTotal()));
        doNothing().when(notificationService).notifyOrderStatusChange(
                eq(pedido.getId()), eq(cliente.getEmail()), eq(cliente.getTelefone()), eq("CONFIRMED"));

        pedidoNotificacaoService.enviarNotificacoesConfirmacaoPedido(pedido);

        verify(notificationService).notifyRestaurantNewOrder(
                eq(pedido.getId()), eq(restaurante.getEmail()), eq(pedido.getTotal()));
        verify(notificationService).notifyOrderStatusChange(
                eq(pedido.getId()), eq(cliente.getEmail()), eq(cliente.getTelefone()), eq("CONFIRMED"));
    }

    @Test
    void deveNotificarClienteSobreStatusPedido() {
        String statusKey = "PREPARING";
        doNothing().when(notificationService).notifyOrderStatusChange(
                anyLong(), anyString(), anyString(), anyString());

        pedidoNotificacaoService.notificarClienteStatusPedido(pedido, statusKey);

        verify(notificationService).notifyOrderStatusChange(
                eq(pedido.getId()), anyString(), anyString(), eq(statusKey));
    }

    @Test
    void deveNotificarClienteQuandoSaiuParaEntrega() {
        pedido.setStatus(StatusPedido.OUT_FOR_DELIVERY);

        doNothing().when(notificationService).notifyOrderStatusChange(
                anyLong(), anyString(), anyString(), anyString());

        pedidoNotificacaoService.notificarClienteStatusPedido(pedido, "OUT_FOR_DELIVERY");

        verify(notificationService).notifyOrderStatusChange(
                eq(pedido.getId()), anyString(), anyString(), eq("OUT_FOR_DELIVERY"));
    }

    @Test
    void deveNotificarClienteQuandoEntregue() {
        pedido.setStatus(StatusPedido.DELIVERED);

        doNothing().when(notificationService).notifyOrderStatusChange(
                anyLong(), anyString(), anyString(), anyString());

        pedidoNotificacaoService.notificarClienteStatusPedido(pedido, "DELIVERED");

        verify(notificationService).notifyOrderStatusChange(
                eq(pedido.getId()), anyString(), anyString(), eq("DELIVERED"));
    }
}
