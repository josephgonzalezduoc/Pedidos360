package cl.pedidos360.orders.pedido;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final CatalogClient catalog;

    public PedidoService(PedidoRepository repository, CatalogClient catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @Transactional
    public Pedido crear(PedidoRequest request) {
        CatalogClient.ProductoCatalogo producto =
                catalog.buscar(request.productoId());

        if (producto.stock() < request.cantidad()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Stock insuficiente");
        }

        Pedido pedido = new Pedido(
                request,
                producto.nombre(),
                producto.precio()
        );

        return repository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Pedido buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }
}