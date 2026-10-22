package cl.pedidos360.notify.config;

public final class RabbitNames {

    public static final String DIRECT = "cmd.direct";
    public static final String TOPIC = "cmd.topic";
    public static final String DLX = "cmd.dead.dlx";

    public static final String EMAIL = "q.cmd.email";
    public static final String KITCHEN = "q.cmd.kitchen";
    public static final String INVOICE = "q.cmd.invoice";

    public static final String EMAIL_KEY = "email.send";
    public static final String KITCHEN_KEY = "kitchen.ticket";
    public static final String INVOICE_KEY = "invoice.gen";

    private RabbitNames() {
    }
}