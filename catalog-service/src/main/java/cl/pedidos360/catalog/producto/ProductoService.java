package cl.pedidos360.catalog.producto;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Producto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Transactional
    public Producto crear(ProductoRequest request) {
        return repository.save(new Producto(
                request.nombre().trim(),
                request.precio(),
                request.stock()
        ));
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarParaModificar(id);

        producto.actualizar(
                request.nombre().trim(),
                request.precio(),
                request.stock()
        );

        return repository.save(producto);
    }

    @Transactional
    public Producto descontarStock(Long id, int cantidad) {
        if (cantidad <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La cantidad debe ser positiva");
        }

        Producto producto = buscarParaModificar(id);

        if (producto.getStock() < cantidad) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Stock insuficiente");
        }

        producto.descontarStock(cantidad);
        return repository.save(producto);
    }

    private Producto buscarParaModificar(Long id) {
        return repository.buscarParaActualizar(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }
}