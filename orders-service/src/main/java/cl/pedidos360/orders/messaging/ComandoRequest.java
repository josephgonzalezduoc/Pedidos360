package cl.pedidos360.orders.messaging;

import jakarta.validation.constraints.NotNull;

public record ComandoRequest(
        @NotNull TipoExchange exchange,
        boolean falloControlado
) {
    public enum TipoExchange {
        DIRECT, TOPIC
    }
}