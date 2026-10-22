package cl.pedidos360.catalog.producto;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    protected Producto() {
    }

    public Producto(String nombre, BigDecimal precio, Integer stock) {
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void actualizar(String nombre, BigDecimal precio, Integer stock) {
    this.nombre = nombre;
    this.precio = precio;
    this.stock = stock;
    }

    public void descontarStock(int cantidad) {
    if (cantidad <= 0 || cantidad > stock) {
        throw new IllegalArgumentException("Cantidad inválida");
    }
    this.stock -= cantidad;
    }
}