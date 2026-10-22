package cl.pedidos360.orders.pedido;

import jakarta.validation.constraints.*;

public record PedidoRequest(
        @NotBlank @Size(max = 100) String cliente,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull @Positive Long productoId,
        @NotNull @Min(1) @Max(1000) Integer cantidad
) {
}