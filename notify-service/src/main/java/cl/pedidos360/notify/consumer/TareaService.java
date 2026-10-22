package cl.pedidos360.notify.consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class TareaService {

    private final Path root;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public TareaService(@Value("${processing.output-directory:./data/tareas}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    // Serializes local file commits; it is not a distributed exactly-once mechanism.
    public synchronized Resultado procesar(String dominio, byte[] body) throws IOException {
        Payload payload = mapper.readValue(body, Payload.class);
        validar(dominio, payload);

        String titulo = switch (dominio) {
            case "EMAIL" -> "Notificacion preparada (sin envio SMTP)";
            case "KITCHEN" -> "Ticket de preparacion de cocina";
            case "INVOICE" -> "Comprobante interno (sin validez tributaria)";
            default -> throw new IllegalArgumentException("Dominio desconocido");
        };

        String contenido = String.join("\n",
                titulo,
                "eventId=" + payload.eventId(),
                "traceId=" + payload.traceId(),
                "correlationId=" + payload.correlationId(),
                "pedidoId=" + payload.pedidoId(),
                "productoId=" + payload.productoId(),
                "cantidad=" + payload.cantidad(),
                "total=" + payload.total().toPlainString(),
                "timestamp=" + payload.timestamp()) + "\n";

        Path directorio = root.resolve(dominio.toLowerCase(Locale.ROOT));
        Files.createDirectories(directorio);
        Path destino = directorio.resolve(payload.eventId() + ".txt");
        if (Files.exists(destino)) {
            if (!Files.readString(destino, StandardCharsets.UTF_8).equals(contenido)) {
                throw new IllegalArgumentException("eventId repetido con contenido diferente");
            }
            return new Resultado(destino.toString(), true);
        }

        Path temporal = Files.createTempFile(directorio, ".task-", ".tmp");
        try {
            Files.writeString(temporal, contenido, StandardCharsets.UTF_8);
            try {
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, destino);
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
        return new Resultado(destino.toString(), false);
    }

    private void validar(String dominio, Payload p) {
        if (p == null || !dominio.equals(p.type()) || p.eventId() == null
                || p.traceId() == null || p.timestamp() == null
                || p.pedidoId() == null || p.pedidoId() <= 0
                || p.productoId() == null || p.productoId() <= 0
                || p.cantidad() == null || p.cantidad() <= 0 || p.cantidad() > 1000
                || p.total() == null || p.total().signum() <= 0
                || !p.pedidoId().toString().equals(p.correlationId())) {
            throw new IllegalArgumentException("Comando incompleto o incompatible con el dominio");
        }
        Instant.parse(p.timestamp());
    }

    public record Resultado(String archivo, boolean duplicado) { }

    public record Payload(String type, UUID eventId, String timestamp, UUID traceId,
                          String correlationId, Long pedidoId, Long productoId,
                          Integer cantidad, BigDecimal total) { }
}
