package com.siseg.service;

import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPedido;
import com.siseg.repository.PedidoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.logging.Logger;

@Component
public class DeliverySimulationService {
    
    private static final Logger logger = Logger.getLogger(DeliverySimulationService.class.getName());
    
    private final PedidoRepository pedidoRepository;
    private final DeliveryMovementService deliveryMovementService;
    
    public DeliverySimulationService(PedidoRepository pedidoRepository, DeliveryMovementService deliveryMovementService) {
        this.pedidoRepository = pedidoRepository;
        this.deliveryMovementService = deliveryMovementService;
    }
    
    @Scheduled(fixedRate = 10000)
    public void simularEntregasAtivas() {
        List<Pedido> pedidosEmEntrega = pedidoRepository.findByStatus(StatusPedido.OUT_FOR_DELIVERY);
        
        if (pedidosEmEntrega.isEmpty()) {
            return;
        }
        
        int pedidosProcessados = 0;
        for (Pedido pedido : pedidosEmEntrega) {
            if (pedido.getEntregador() != null) {
                try {
                    deliveryMovementService.simularMovimento(pedido.getId());
                    pedidosProcessados++;
                } catch (Exception e) {
                    logger.warning("Erro ao simular movimento para pedido " + pedido.getId() + ": " + e.getMessage());
                }
            }
        }
        
        if (pedidosProcessados > 0) {
            logger.info("Simulação de entregas: " + pedidosProcessados + " pedidos processados");
        }
    }
}

