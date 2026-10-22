package cl.pedidos360.catalog.producto;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Producto> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Producto buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Producto> crear(
            @Valid @RequestBody ProductoRequest request) {

        Producto producto = service.crear(request);

        return ResponseEntity
                .created(URI.create("/api/productos/" + producto.getId()))
                .body(producto);
    }

    @PutMapping("/{id}")
    public Producto actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {

        return service.actualizar(id, request);
    }

    @PostMapping("/{id}/stock/descontar")
    public Producto descontarStock(
            @PathVariable Long id,
            @Valid @RequestBody StockRequest request) {

        return service.descontarStock(id, request.cantidad());
    }
}