package cl.pedidos360.notify.consumer;

import cl.pedidos360.notify.config.RabbitNames;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InvoiceConsumer {

    private final ProcesadorMensaje procesador;

    public InvoiceConsumer(ProcesadorMensaje procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = RabbitNames.INVOICE, ackMode = "MANUAL")
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar("INVOICE", message, channel);
    }
}