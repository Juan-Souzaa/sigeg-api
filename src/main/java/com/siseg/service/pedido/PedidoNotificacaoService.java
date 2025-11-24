package com.siseg.service.pedido;

import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.service.NotificationService;
import org.springframework.stereotype.Service;

@Service
public class PedidoNotificacaoService {

    private final NotificationService notificationService;

    public PedidoNotificacaoService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void enviarNotificacoesConfirmacaoPedido(Pedido pedido) {
        notificarClienteStatusPedido(pedido, "CONFIRMED");
        notificarRestauranteNovoPedido(pedido);
    }

    public void enviarNotificacoesEntregaPedido(Pedido pedido) {
        notificarClienteStatusPedido(pedido, "DELIVERED");
        notificarRestauranteNovoPedido(pedido);
    }

    public void enviarNotificacoesAceitePedido(Pedido pedido) {
        notificarRestauranteNovoPedido(pedido);
        notificarClienteStatusPedido(pedido, "ACEITO_POR_ENTREGADOR");
    }

    public void notificarClienteStatusPedido(Pedido pedido, String status) {
        if (pedido.getCliente() != null) {
            notificationService.notifyOrderStatusChange(
                pedido.getId(),
                pedido.getCliente().getEmail(),
                pedido.getCliente().getTelefone(),
                status
            );
        }
    }

    public void notificarRestauranteNovoPedido(Pedido pedido) {
        if (pedido.getRestaurante() != null) {
            notificationService.notifyRestaurantNewOrder(
                pedido.getId(),
                pedido.getRestaurante().getEmail(),
                pedido.getTotal()
            );
        }
    }

    public void notificarPedidoDisponivelParaEntregador(Pedido pedido, Entregador entregador) {
        String enderecoStr = pedido.getEnderecoEntrega() != null
                ? pedido.getEnderecoEntrega().toGeocodingString()
                : "Endereço não disponível";

        notificationService.notifyNewOrderAvailable(
            pedido.getId(),
            entregador.getEmail(),
            entregador.getTelefone(),
            enderecoStr,
            pedido.getTotal()
        );
    }
}

