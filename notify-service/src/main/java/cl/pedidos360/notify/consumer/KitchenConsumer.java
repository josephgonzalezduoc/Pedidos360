package cl.pedidos360.notify.consumer;

import cl.pedidos360.notify.config.RabbitNames;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class KitchenConsumer {

    private final ProcesadorMensaje procesador;

    public KitchenConsumer(ProcesadorMensaje procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = RabbitNames.KITCHEN, ackMode = "MANUAL")
    public void recibir(Message message, Channel channel) throws IOException {
        procesador.procesar("KITCHEN", message, channel);
    }
}