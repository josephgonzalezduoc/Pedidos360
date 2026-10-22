package cl.pedidos360.notify.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ProcesadorMensaje {

    private static final Logger log =
            LoggerFactory.getLogger(ProcesadorMensaje.class);

    private final TareaService tareas;

    public ProcesadorMensaje(TareaService tareas) {
        this.tareas = tareas;
    }

    public void procesar(
            String dominio, Message message, Channel channel)
            throws IOException {

        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            String contenido = new String(
                    message.getBody(), StandardCharsets.UTF_8);

            if (contenido.isBlank()) {
                throw new IllegalArgumentException("Mensaje vacío");
            }

            Object fallo = message.getMessageProperties()
                    .getHeaders().get("x-force-failure");

            if (Boolean.parseBoolean(String.valueOf(fallo))) {
                throw new IllegalStateException("Fallo controlado de prueba");
            }

            TareaService.Resultado resultado = tareas.procesar(dominio, message.getBody());

            log.info("Resultado persistido: archivo={}, duplicado={}",
                    resultado.archivo(), resultado.duplicado());

            log.info(
                    "Tarea procesada: dominio={}, messageId={}, bytes={}",
                    dominio,
                    message.getMessageProperties().getMessageId(),
                    message.getBody().length
            );

        } catch (RuntimeException | IOException ex) {
            boolean reintentar = !Boolean.TRUE.equals(
                    message.getMessageProperties().isRedelivered());

            log.error(
                    "Error: dominio={}, destino={}, motivo={}",
                    dominio,
                    reintentar ? "REINTENTO" : "DLQ",
                    ex.getMessage()
            );

            channel.basicNack(tag, false, reintentar);
            return;
        }

        channel.basicAck(tag, false);
        log.info("ACK: dominio={}, deliveryTag={}", dominio, tag);
    }
}