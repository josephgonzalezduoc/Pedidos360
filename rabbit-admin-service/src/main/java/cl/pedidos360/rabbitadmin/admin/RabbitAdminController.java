package cl.pedidos360.rabbitadmin.admin;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> crearCola(
            @Valid @RequestBody AdminRequests.QueueRequest request) {

        service.crearCola(request);

        return ResponseEntity
                .created(URI.create("/api/admin/queues/" + request.nombre()))
                .body(Map.of("nombre", request.nombre(), "estado", "DECLARADA"));
    }

    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String nombre) {
        service.eliminarCola(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(
            @Valid @RequestBody AdminRequests.ExchangeRequest request) {

        service.crearExchange(request);

        return ResponseEntity
                .created(URI.create("/api/admin/exchanges/" + request.nombre()))
                .body(Map.of("nombre", request.nombre(), "estado", "DECLARADO"));
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String nombre) {
        service.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> crearBinding(
            @Valid @RequestBody AdminRequests.BindingRequest request) {

        service.crearBinding(request);

        return ResponseEntity.status(201)
                .body(Map.of("estado", "BINDING_DECLARADO"));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(
            @Valid @RequestBody AdminRequests.BindingRequest request) {

        service.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}