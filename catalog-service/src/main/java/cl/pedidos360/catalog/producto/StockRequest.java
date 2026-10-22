package cl.pedidos360.catalog.producto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockRequest(
        @NotNull @Min(1) Integer cantidad
) {
}