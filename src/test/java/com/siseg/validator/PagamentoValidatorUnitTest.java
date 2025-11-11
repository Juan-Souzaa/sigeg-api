package com.siseg.validator;

import com.siseg.model.Pedido;
import com.siseg.model.enumerations.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PagamentoValidatorUnitTest {

    @InjectMocks
    private PagamentoValidator pagamentoValidator;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        pedido = new Pedido();
        pedido.setId(1L);
    }

    @Test
    void deveValidarStatusPedidoQuandoCreated() {
        pedido.setStatus(StatusPedido.CREATED);

        assertDoesNotThrow(() -> pagamentoValidator.validateStatusPedido(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoPedidoDelivered() {
        pedido.setStatus(StatusPedido.DELIVERED);

        assertThrows(RuntimeException.class, 
                () -> pagamentoValidator.validateStatusPedido(pedido));
    }

    @Test
    void deveLancarExcecaoQuandoPedidoCanceled() {
        pedido.setStatus(StatusPedido.CANCELED);

        assertThrows(RuntimeException.class, 
                () -> pagamentoValidator.validateStatusPedido(pedido));
    }
}

