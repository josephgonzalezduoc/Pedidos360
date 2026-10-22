package cl.pedidos360.orders.messaging;

import cl.pedidos360.orders.pedido.Pedido;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PedidoProducer {

    private final RabbitTemplate rabbit;
    private final String directExchange;
    private final String topicExchange;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public PedidoProducer(
            RabbitTemplate rabbit,
            @Value("${messaging.exchange.direct}") String directExchange,
            @Value("${messaging.exchange.topic}") String topicExchange) {

        this.rabbit = rabbit;
        this.directExchange = directExchange;
        this.topicExchange = topicExchange;
    }

    public Map<String, String> publicar(
            Pedido pedido,
            ComandoDestino destino,
            ComandoRequest request) {

        String eventId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();

        String exchange =
                request.exchange() == ComandoRequest.TipoExchange.DIRECT
                        ? directExchange : topicExchange;

        String routingKey = destino.routingKey();

        Map<String, Object> contenido = Map.of(
                "type", destino.name(),
                "eventId", eventId,
                "timestamp", Instant.now().toString(),
                "traceId", traceId,
                "correlationId", pedido.getId().toString(),
                "pedidoId", pedido.getId(),
                "productoId", pedido.getProductoId(),
                "cantidad", pedido.getCantidad(),
                "total", pedido.getTotal()
        );

        try {
            MessageProperties properties = new MessageProperties();
            properties.setContentType("application/json");
            properties.setContentEncoding("UTF-8");
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            properties.setMessageId(eventId);
            properties.setCorrelationId(pedido.getId().toString());
            properties.setHeader("x-force-failure", request.falloControlado());

            Message message = new Message(
                    mapper.writeValueAsBytes(contenido), properties);

            CorrelationData correlation = new CorrelationData(eventId);

            rabbit.send(exchange, routingKey, message, correlation);

            CorrelationData.Confirm confirm =
                    correlation.getFuture().get(8, TimeUnit.SECONDS);

            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El broker no confirmó el enrutamiento del mensaje");
            }

            return Map.of(
                    "eventId", eventId,
                    "exchange", exchange,
                    "routingKey", routingKey,
                    "estado", "CONFIRMADO_POR_BROKER"
            );

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Publicación interrumpida");
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo confirmar la publicación");
        }
    }
}