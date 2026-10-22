package cl.pedidos360.notify.dlq;

import cl.pedidos360.notify.config.RabbitNames;
import com.rabbitmq.client.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class DlqService {

    private static final Logger log =
            LoggerFactory.getLogger(DlqService.class);

    private final RabbitTemplate rabbit;

    public DlqService(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public enum Dominio {
        EMAIL, KITCHEN, INVOICE
    }

    public Map<String, Object> inspeccionar(Dominio dominio) {
        String cola = switch (dominio) {
            case EMAIL -> RabbitNames.EMAIL + ".dlq";
            case KITCHEN -> RabbitNames.KITCHEN + ".dlq";
            case INVOICE -> RabbitNames.INVOICE + ".dlq";
        };

        try {
            return rabbit.execute(channel -> {
                GetResponse respuesta = channel.basicGet(cola, false);

                if (respuesta == null) {
                    return Map.<String, Object>of(
                            "cola", cola,
                            "encontrado", false
                    );
                }

                String contenido = new String(
                        respuesta.getBody(), StandardCharsets.UTF_8);

                String messageId = String.valueOf(
                        respuesta.getProps().getMessageId());

                Map<String, Object> headers =
                        respuesta.getProps().getHeaders();

                String motivo = headers == null
                        ? ""
                        : String.valueOf(headers.get("x-death"));

                // Reencola en la misma DLQ para conservar la evidencia.
                channel.basicNack(
                        respuesta.getEnvelope().getDeliveryTag(),
                        false,
                        true
                );

                log.warn(
                        "Inspección DLQ: cola={}, messageId={}, contenido={}",
                        cola,
                        messageId,
                        contenido.replace('\n', ' ').replace('\r', ' ')
                );

                return Map.<String, Object>of(
                        "cola", cola,
                        "encontrado", true,
                        "messageId", messageId,
                        "contenido", contenido,
                        "xDeath", motivo,
                        "conservadoEnDlq", true
                );
            });

        } catch (AmqpException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo inspeccionar la DLQ");
        }
    }
}