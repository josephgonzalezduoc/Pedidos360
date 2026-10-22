package cl.pedidos360.notify.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TareaServiceTests {

    @TempDir Path directory;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private TareaService.Payload payload(String type, UUID id, int cantidad) {
        return new TareaService.Payload(type, id, "2026-10-22T04:00:00Z",
                UUID.fromString("d2eebfa6-f28c-42a2-9a01-a3c60f2bdfd8"),
                "1", 1L, 2L, cantidad, new BigDecimal("12980.00"));
    }

    @Test
    void generaResultadoParaCadaDominio() throws Exception {
        TareaService service = new TareaService(directory.toString());
        for (String domain : new String[]{"EMAIL", "KITCHEN", "INVOICE"}) {
            var result = service.procesar(domain,
                    mapper.writeValueAsBytes(payload(domain, UUID.randomUUID(), 2)));
            assertFalse(result.duplicado());
            assertTrue(Files.readString(Path.of(result.archivo())).contains("pedidoId=1"));
        }
    }

    @Test
    void redeliveryConservaUnSoloArchivoInclusoTrasRecrearServicio() throws Exception {
        byte[] body = mapper.writeValueAsBytes(payload("EMAIL", UUID.randomUUID(), 2));
        var first = new TareaService(directory.toString()).procesar("EMAIL", body);
        var second = new TareaService(directory.toString()).procesar("EMAIL", body);
        assertFalse(first.duplicado());
        assertTrue(second.duplicado());
        assertEquals(first.archivo(), second.archivo());
        try (var files = Files.list(Path.of(first.archivo()).getParent())) {
            assertEquals(1L, files.count());
        }
    }

    @Test
    void mismoIdConOtroContenidoSeRechazaSinSobrescribir() throws Exception {
        TareaService service = new TareaService(directory.toString());
        UUID id = UUID.randomUUID();
        var first = service.procesar("EMAIL", mapper.writeValueAsBytes(payload("EMAIL", id, 2)));
        assertThrows(IllegalArgumentException.class, () -> service.procesar("EMAIL",
                mapper.writeValueAsBytes(payload("EMAIL", id, 3))));
        assertTrue(Files.readString(Path.of(first.archivo())).contains("cantidad=2"));
    }

    @Test
    void dominioErroneoYCantidadInvalidaNoGeneranArchivos() {
        TareaService service = new TareaService(directory.toString());
        assertThrows(IllegalArgumentException.class, () -> service.procesar("EMAIL",
                mapper.writeValueAsBytes(payload("KITCHEN", UUID.randomUUID(), 2))));
        assertThrows(IllegalArgumentException.class, () -> service.procesar("EMAIL",
                mapper.writeValueAsBytes(payload("EMAIL", UUID.randomUUID(), 0))));
        assertFalse(Files.exists(directory.resolve("email")));
    }
}
