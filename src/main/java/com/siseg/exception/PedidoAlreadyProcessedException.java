package com.siseg.exception;

public class PedidoAlreadyProcessedException extends RuntimeException {
    public PedidoAlreadyProcessedException(String message) {
        super(message);
    }
}
