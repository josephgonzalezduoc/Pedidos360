package cl.pedidos360.rabbitadmin.admin;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RabbitAdminService {

    private final RabbitAdmin admin;
    private final RabbitTemplate rabbit;

    public RabbitAdminService(RabbitAdmin admin, RabbitTemplate rabbit) {
        this.admin = admin;
        this.rabbit = rabbit;
    }

    public void crearCola(AdminRequests.QueueRequest request) {
        QueueBuilder builder = QueueBuilder.durable(request.nombre());

        if (request.ttlMs() != null) {
            builder.withArgument("x-message-ttl", request.ttlMs());
        }

        if (request.maxLength() != null) {
            builder.withArgument("x-max-length", request.maxLength());
        }

        admin.declareQueue(builder.build());
    }

    public void crearExchange(AdminRequests.ExchangeRequest request) {
        Exchange exchange = switch (request.tipo()) {
            case DIRECT ->
                    new DirectExchange(request.nombre(), true, false);
            case TOPIC ->
                    new TopicExchange(request.nombre(), true, false);
        };

        admin.declareExchange(exchange);
    }

    public void crearBinding(AdminRequests.BindingRequest request) {
        admin.declareBinding(binding(request));
    }

    public void eliminarBinding(AdminRequests.BindingRequest request) {
        admin.removeBinding(binding(request));
    }

    public void eliminarCola(String nombre) {
        validarNombre(nombre);

        // Solo elimina si no tiene consumidores ni mensajes.
        admin.deleteQueue(nombre, true, true);
    }

    public void eliminarExchange(String nombre) {
        validarNombre(nombre);

        rabbit.execute(channel -> {
            // Solo elimina si no tiene bindings asociados.
            channel.exchangeDelete(nombre, true);
            return null;
        });
    }

    private Binding binding(AdminRequests.BindingRequest request) {
        return new Binding(
                request.cola(),
                Binding.DestinationType.QUEUE,
                request.exchange(),
                request.routingKey(),
                null
        );
    }

    private void validarNombre(String nombre) {
        if (nombre == null
                || !nombre.matches(AdminRequests.NAME_PATTERN)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Nombre inválido");
        }
    }
}