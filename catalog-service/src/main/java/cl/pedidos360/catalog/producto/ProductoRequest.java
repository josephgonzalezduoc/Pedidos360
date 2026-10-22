package cl.pedidos360.catalog.producto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoRequest(

    @NotBlank
    @Size(max = 100)
    String nombre,

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    BigDecimal precio,

    @NotNull
    @Min(0)
    Integer stock

) {
}