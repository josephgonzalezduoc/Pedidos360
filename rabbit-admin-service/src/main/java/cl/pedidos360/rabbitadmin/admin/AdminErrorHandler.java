package cl.pedidos360.rabbitadmin.admin;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AdminErrorHandler {

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, String>> manejar(AmqpException ex) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String mensaje = "No se pudo completar la operación en RabbitMQ";

        Throwable causa = ex;

        while (causa != null) {
            if (causa instanceof ShutdownSignalException signal
                    && signal.getReason() instanceof AMQP.Channel.Close close) {

                switch (close.getReplyCode()) {
                    case 404 -> {
                        status = HttpStatus.NOT_FOUND;
                        mensaje = "La cola o el exchange no existe";
                    }
                    case 405, 406 -> {
                        status = HttpStatus.CONFLICT;
                        mensaje = "Configuración incompatible o recurso en uso";
                    }
                    case 403 -> {
                        status = HttpStatus.FORBIDDEN;
                        mensaje = "RabbitMQ rechazó los permisos de la operación";
                    }
                    default -> {
                    }
                }
                break;
            }

            causa = causa.getCause();
        }

        return ResponseEntity.status(status)
                .body(Map.of("error", mensaje));
    }
}