package cl.pedidos360.orders.messaging;

public enum ComandoDestino {

    EMAIL("email.send"),
    KITCHEN("kitchen.ticket"),
    INVOICE("invoice.gen");

    private final String routingKey;

    ComandoDestino(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }
}