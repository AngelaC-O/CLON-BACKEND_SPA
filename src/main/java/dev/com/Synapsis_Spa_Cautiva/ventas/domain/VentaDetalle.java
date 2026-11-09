package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * TRG_VENTA_DETALLE_REGLAS reescribe precio, descuento, subtotal y comisión: Hibernate los vuelve a leer al guardar.
 */
@Entity
@Table(name = "VENTA_DETALLE")
@DynamicUpdate
@Getter
public class VentaDetalle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_detalle")
	private Long idDetalle;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_venta", nullable = false)
	private Venta venta;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_item", nullable = false, length = 10)
	private TipoItemVentaDetalle tipoItem;

	@Column(name = "id_atencion")
	private Long idAtencion;

	@Column(name = "id_producto")
	private Long idProducto;

	@Column(name = "id_paquete")
	private Long idPaquete;

	@Column(name = "id_promocion")
	private Long idPromocion;

	@Column(name = "cantidad", nullable = false)
	private Integer cantidad;

	@Generated(event = { EventType.INSERT, EventType.UPDATE }, writable = true)
	@Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
	private BigDecimal precioUnitario;

	@Generated(event = { EventType.INSERT, EventType.UPDATE }, writable = true)
	@Column(name = "descuento", nullable = false, precision = 10, scale = 2)
	private BigDecimal descuento;

	@Generated(event = { EventType.INSERT, EventType.UPDATE }, writable = true)
	@Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
	private BigDecimal subtotal;

	@Generated(event = { EventType.INSERT, EventType.UPDATE }, writable = true)
	@Column(name = "id_personal")
	private Long idPersonal;

	@Generated(event = { EventType.INSERT, EventType.UPDATE }, writable = true)
	@Column(name = "porcentaje_comision", precision = 5, scale = 2)
	private BigDecimal porcentajeComision;

	@Generated(event = { EventType.INSERT, EventType.UPDATE }, writable = true)
	@Column(name = "monto_comision", precision = 10, scale = 2)
	private BigDecimal montoComision;

	protected VentaDetalle() {
	}

	VentaDetalle(Venta venta, Long idAtencion, BigDecimal precioUnitario, Long idPersonal) {
		this.venta = venta;
		this.tipoItem = TipoItemVentaDetalle.SERVICIO;
		this.idAtencion = idAtencion;
		this.cantidad = 1;
		this.precioUnitario = precioUnitario;
		this.descuento = BigDecimal.ZERO;
		this.subtotal = precioUnitario;
		this.idPersonal = idPersonal;
	}

	/** Línea de producto (RF38): el precio lo pone TRG_VENTA_DETALLE_REGLAS con el de PRODUCTO. */
	static VentaDetalle producto(Venta venta, Long idProducto, Integer cantidad, BigDecimal precioUnitario, Long idPersonal) {
		VentaDetalle detalle = new VentaDetalle();
		detalle.venta = venta;
		detalle.tipoItem = TipoItemVentaDetalle.PRODUCTO;
		detalle.idProducto = idProducto;
		detalle.cantidad = cantidad;
		detalle.precioUnitario = precioUnitario;
		detalle.descuento = BigDecimal.ZERO;
		detalle.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
		detalle.idPersonal = idPersonal;
		return detalle;
	}

	void aplicarPromocion(Long idPromocion) {
		this.idPromocion = idPromocion;
	}

	public Long getIdVenta() {
		return venta.getIdVenta();
	}

}
