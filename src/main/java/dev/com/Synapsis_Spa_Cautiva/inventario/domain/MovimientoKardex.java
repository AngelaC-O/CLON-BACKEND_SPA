package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import java.time.LocalDateTime;

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
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Los movimientos de venta y compra los inserta Oracle (TRG_VENTA_EFECTOS, TRG_COMPRA_DETALLE_KARDEX). En los que inserta
 * Java, TRG_KARDEX_APLICAR fija usuario, fecha y saldo y mueve INVENTARIO_SEDE: Hibernate los vuelve a leer al guardar.
 */
@Entity
@Table(name = "MOVIMIENTO_KARDEX")
@Getter
public class MovimientoKardex {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento")
	private Long idMovimiento;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10)
	private TipoMovimientoKardex tipo;

	@Column(name = "motivo", nullable = false, length = 150)
	private String motivo;

	@Column(name = "cantidad", nullable = false)
	private Integer cantidad;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "saldo", nullable = false)
	private Integer saldo;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime fechaHora;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "id_detalle_venta")
	private Long idDetalleVenta;

	@Column(name = "id_detalle_compra")
	private Long idDetalleCompra;

	@Column(name = "id_atencion")
	private Long idAtencion;

	protected MovimientoKardex() {
	}

	private MovimientoKardex(Producto producto, Long idSede, TipoMovimientoKardex tipo, String motivo, Integer cantidad,
			Long idUsuario, Long idAtencion) {
		this.producto = producto;
		this.idSede = idSede;
		this.tipo = tipo;
		this.motivo = motivo;
		this.cantidad = cantidad;
		this.saldo = 0;
		this.fechaHora = LocalDateTime.now();
		this.idUsuario = idUsuario;
		this.idAtencion = idAtencion;
	}

	/** Salida de insumo (RF46); el usuario es el de PKG_SESION, que pone TRG_KARDEX_APLICAR. */
	static MovimientoKardex salida(Producto producto, Long idSede, Integer cantidad, String motivo, Long idAtencion) {
		return new MovimientoKardex(producto, idSede, TipoMovimientoKardex.SALIDA, motivo, cantidad, null, idAtencion);
	}

	/** Ajuste por conteo físico (RF48): la cantidad es la diferencia con signo. */
	static MovimientoKardex ajuste(Producto producto, Long idSede, Integer diferencia, String motivo, Long idUsuario) {
		return new MovimientoKardex(producto, idSede, TipoMovimientoKardex.AJUSTE, motivo, diferencia, idUsuario, null);
	}

	/** Avisa al producto el movimiento que quedó guardado (puede ser la copia que hizo merge). */
	@PostPersist
	void alGuardar() {
		producto.alGuardarMovimiento(this);
	}

	public Long getIdProducto() {
		return producto.getIdProducto();
	}

}
