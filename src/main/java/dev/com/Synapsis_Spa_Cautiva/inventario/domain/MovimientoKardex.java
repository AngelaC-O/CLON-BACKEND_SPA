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
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Los movimientos de venta y compra los inserta Oracle (TRG_VENTA_EFECTOS, TRG_COMPRA_DETALLE_KARDEX).
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

	@Column(name = "saldo", nullable = false)
	private Integer saldo;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime fechaHora;

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

	public Long getIdProducto() {
		return producto.getIdProducto();
	}

}
