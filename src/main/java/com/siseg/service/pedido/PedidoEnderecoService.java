package com.siseg.service.pedido;

import com.siseg.dto.pedido.PedidoRequestDTO;
import com.siseg.exception.ResourceNotFoundException;
import com.siseg.model.Cliente;
import com.siseg.model.Endereco;
import com.siseg.model.Entregador;
import com.siseg.model.Pedido;
import com.siseg.model.Restaurante;
import com.siseg.repository.EntregadorRepository;
import com.siseg.service.EnderecoService;
import com.siseg.util.TempoEstimadoCalculator;
import com.siseg.util.VehicleConstants;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class PedidoEnderecoService {

    private static final Logger logger = Logger.getLogger(PedidoEnderecoService.class.getName());

    private final EnderecoService enderecoService;
    private final TempoEstimadoCalculator tempoEstimadoCalculator;
    private final EntregadorRepository entregadorRepository;

    public PedidoEnderecoService(EnderecoService enderecoService,
                                 TempoEstimadoCalculator tempoEstimadoCalculator,
                                 EntregadorRepository entregadorRepository) {
        this.enderecoService = enderecoService;
        this.tempoEstimadoCalculator = tempoEstimadoCalculator;
        this.entregadorRepository = entregadorRepository;
    }

    public void processarEnderecoEntrega(Pedido pedido, Cliente cliente, PedidoRequestDTO dto) {
        Endereco enderecoEntrega;

        if (dto.getEnderecoId() != null) {
            enderecoEntrega = enderecoService.buscarEnderecoPorIdECliente(dto.getEnderecoId(), cliente.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado ou não pertence ao cliente"));
        } else {
            enderecoEntrega = enderecoService.buscarEnderecoPrincipalCliente(cliente.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não possui endereço cadastrado"));
        }

        pedido.setEnderecoEntrega(enderecoEntrega);

        if (enderecoEntrega.getLatitude() == null || enderecoEntrega.getLongitude() == null) {
            enderecoService.geocodificarESalvar(enderecoEntrega);
        }
    }

    public void inicializarPosicaoEntregadorSeNecessario(Pedido pedido) {
        if (pedido.getEntregador() == null) {
            return;
        }

        Entregador entregador = pedido.getEntregador();
        if (precisaInicializarPosicao(entregador, pedido.getRestaurante())) {
            definirPosicaoInicialEntregador(entregador, pedido.getRestaurante());
        }
    }

    public void calcularEAtualizarTempoEstimadoEntrega(Pedido pedido, Entregador entregador) {
        if (!temCoordenadasCompletas(pedido)) {
            aplicarTempoPadraoEntrega(pedido);
            return;
        }

        Optional<Endereco> enderecoRestaurante = pedido.getRestaurante().getEnderecoPrincipal();
        Endereco enderecoEntrega = pedido.getEnderecoEntrega();

        if (enderecoRestaurante.isEmpty() || enderecoEntrega == null) {
            aplicarTempoPadraoEntrega(pedido);
            return;
        }

        var resultado = tempoEstimadoCalculator.calculateDistanceAndTime(
            enderecoRestaurante.get().getLatitude(),
            enderecoRestaurante.get().getLongitude(),
            enderecoEntrega.getLatitude(),
            enderecoEntrega.getLongitude(),
            entregador.getTipoVeiculo()
        );

        if (resultado.getDistanciaKm() != null && resultado.getTempoMinutos() > 0) {
            Duration tempoEstimado = Duration.ofMinutes(resultado.getTempoMinutos());
            pedido.setTempoEstimadoEntrega(Instant.now().plus(tempoEstimado));
            logger.info(String.format("Tempo estimado calculado: %d minutos para distância de %s km (OSRM: %s)",
                resultado.getTempoMinutos(), resultado.getDistanciaKm(), resultado.isUsadoOSRM()));
        } else {
            aplicarTempoPadraoEntrega(pedido);
        }
    }

    private boolean temCoordenadasCompletas(Pedido pedido) {
        if (pedido.getRestaurante() == null || pedido.getEnderecoEntrega() == null) {
            return false;
        }

        Optional<Endereco> enderecoRestaurante = pedido.getRestaurante().getEnderecoPrincipal();
        Endereco enderecoEntrega = pedido.getEnderecoEntrega();

        return enderecoRestaurante.isPresent() &&
               enderecoRestaurante.get().getLatitude() != null &&
               enderecoRestaurante.get().getLongitude() != null &&
               enderecoEntrega.getLatitude() != null &&
               enderecoEntrega.getLongitude() != null;
    }

    private void aplicarTempoPadraoEntrega(Pedido pedido) {
        Duration tempoEstimado = Duration.ofMinutes(VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS);
        pedido.setTempoEstimadoEntrega(Instant.now().plus(tempoEstimado));
        logger.warning("Coordenadas não disponíveis, usando tempo padrão de "
            + VehicleConstants.TEMPO_PADRAO_ENTREGA_MINUTOS + " minutos");
    }

    private boolean precisaInicializarPosicao(Entregador entregador, Restaurante restaurante) {
        if (restaurante == null) {
            return false;
        }

        return restaurante.getEnderecoPrincipal()
                .map(endereco -> endereco.getLatitude() != null && endereco.getLongitude() != null)
                .orElse(false);
    }

    private void definirPosicaoInicialEntregador(Entregador entregador, Restaurante restaurante) {
        restaurante.getEnderecoPrincipal()
                .ifPresentOrElse(
                    endereco -> {
                        if (endereco.getLatitude() != null && endereco.getLongitude() != null) {
                            entregador.setLatitude(endereco.getLatitude());
                            entregador.setLongitude(endereco.getLongitude());
                            entregadorRepository.save(entregador);
                            logger.info("Posição inicial do entregador definida com coordenadas do restaurante para iniciar entrega");
                        }
                    },
                    () -> logger.warning("Restaurante não possui endereço principal com coordenadas")
                );
    }
}

