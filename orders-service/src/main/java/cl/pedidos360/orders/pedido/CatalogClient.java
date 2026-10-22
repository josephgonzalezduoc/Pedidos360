package cl.pedidos360.orders.pedido;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Component
public class CatalogClient {

    private final RestClient client;

    public CatalogClient(@Value("${catalog.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public ProductoCatalogo buscar(Long id) {
        try {
            ProductoCatalogo producto = client.get()
                    .uri("/api/productos/{id}", id)
                    .retrieve()
                    .body(ProductoCatalogo.class);

            if (producto == null
                    || producto.id() == null
                    || !id.equals(producto.id())
                    || producto.nombre() == null
                    || producto.precio() == null
                    || producto.precio().signum() <= 0
                    || producto.stock() == null
                    || producto.stock() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Respuesta inválida del catálogo");
            }

            return producto;

        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado");
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El catálogo respondió con un error");

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar el catálogo");
        }
    }

    public record ProductoCatalogo(
            Long id,
            String nombre,
            BigDecimal precio,
            Integer stock
    ) {
    }
}