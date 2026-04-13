package com.crm.zonas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cotizaciones")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "numero_cotizacion", nullable = false, unique = true, length = 30)
    private String numeroCotizacion;

    // ── Propietarios (tres roles distintos, misma tabla) ──────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Propietario propietario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id", nullable = false)
    private Propietario creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Propietario vendedor;

    // ── Cliente / facturación ─────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "punto_envio", length = 200)
    private String puntoEnvio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_cliente_id")
    private TipoCliente tipoCliente;

    // ── Operación ─────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_operacion_id")
    private CentroOperacion centroOperacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_id")
    private ListaPrecio listaPrecio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condicion_pago_id")
    private CondicionPago condicionPago;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    // ── Valores monetarios ────────────────────────────────────────
    @Column(name = "valor_bruto",            precision = 18, scale = 2)
    private BigDecimal valorBruto;

    @Column(name = "valor_subtotal",         precision = 18, scale = 2)
    private BigDecimal valorSubtotal;

    @Column(name = "impuestos",              precision = 18, scale = 2)
    private BigDecimal impuestos;

    @Column(name = "descuentos",             precision = 18, scale = 2)
    private BigDecimal descuentos;

    @Column(name = "descuento_global_pct",   precision = 6,  scale = 2)
    private BigDecimal descuentoGlobalPct;

    @Column(name = "valor_descuento_global", precision = 18, scale = 2)
    private BigDecimal valorDescuentoGlobal;

    @Column(name = "valor_total",            precision = 18, scale = 2)
    private BigDecimal valorTotal;

    // ── Vínculos ERP ──────────────────────────────────────────────
    @Column(name = "pedido_erp", length = 30)
    private String pedidoErp;

    @Column(name = "rowid_erp", precision = 12, scale = 0)
    private BigDecimal rowidErp;

    // ── Notas ─────────────────────────────────────────────────────
    @Column(name = "notas_pedido", columnDefinition = "TEXT")
    private String notasPedido;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}