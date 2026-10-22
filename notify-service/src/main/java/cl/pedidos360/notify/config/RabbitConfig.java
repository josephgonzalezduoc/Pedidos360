package cl.pedidos360.notify.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange comandosDirect() {
        return new DirectExchange(RabbitNames.DIRECT, true, false);
    }

    @Bean
    public TopicExchange comandosTopic() {
        return new TopicExchange(RabbitNames.TOPIC, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitNames.DLX, true, false);
    }

    @Bean
    public Declarables emailTopology() {
        return crearFlujo(
                RabbitNames.EMAIL,
                RabbitNames.EMAIL_KEY,
                "email.#"
        );
    }

    @Bean
    public Declarables kitchenTopology() {
        return crearFlujo(
                RabbitNames.KITCHEN,
                RabbitNames.KITCHEN_KEY,
                "kitchen.#"
        );
    }

    @Bean
    public Declarables invoiceTopology() {
        return crearFlujo(
                RabbitNames.INVOICE,
                RabbitNames.INVOICE_KEY,
                "invoice.#"
        );
    }

    private Declarables crearFlujo(
            String nombre, String routingKey, String patron) {

        Queue principal = QueueBuilder.durable(nombre)
                .deadLetterExchange(RabbitNames.DLX)
                .deadLetterRoutingKey(routingKey)
                .build();

        Queue dlq = QueueBuilder.durable(nombre + ".dlq").build();

        Binding directBinding = new Binding(
                nombre,
                Binding.DestinationType.QUEUE,
                RabbitNames.DIRECT,
                routingKey,
                null
        );

        Binding topicBinding = new Binding(
                nombre,
                Binding.DestinationType.QUEUE,
                RabbitNames.TOPIC,
                patron,
                null
        );

        Binding deadBinding = new Binding(
                nombre + ".dlq",
                Binding.DestinationType.QUEUE,
                RabbitNames.DLX,
                routingKey,
                null
        );

        return new Declarables(
                principal, dlq,
                directBinding, topicBinding, deadBinding
        );
    }
}