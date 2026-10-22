package cl.pedidos360.orders.pedido;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String cliente;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 100)
    private String productoNombre;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(nullable = false)
    private Instant creadoEn;

    protected Pedido() {
    }

    public Pedido(PedidoRequest request, String productoNombre,
                  BigDecimal precioUnitario) {
        this.cliente = request.cliente().trim();
        this.email = request.email().trim();
        this.productoId = request.productoId();
        this.productoNombre = productoNombre;
        this.cantidad = request.cantidad();
        this.precioUnitario = precioUnitario;
        this.total = precioUnitario.multiply(
                BigDecimal.valueOf(request.cantidad()));
        this.estado = "CREADO";
        this.creadoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public String getEmail() {
        return email;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}