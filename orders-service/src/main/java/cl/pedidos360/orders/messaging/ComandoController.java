package cl.pedidos360.orders.messaging;

import cl.pedidos360.orders.pedido.Pedido;
import cl.pedidos360.orders.pedido.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class ComandoController {

    private final PedidoService pedidos;
    private final PedidoProducer producer;

    public ComandoController(
            PedidoService pedidos, PedidoProducer producer) {
        this.pedidos = pedidos;
        this.producer = producer;
    }

    @PostMapping("/{id}/comandos/{destino}")
    public ResponseEntity<Map<String, String>> publicar(
            @PathVariable Long id,
            @PathVariable ComandoDestino destino,
            @Valid @RequestBody ComandoRequest request) {

        Pedido pedido = pedidos.buscar(id);

        return ResponseEntity.accepted()
                .body(producer.publicar(pedido, destino, request));
    }
}