package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "VENTA_PAGO")
@Getter
public class VentaPago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pago")
	private Long idPago;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_venta", nullable = false)
	private Venta venta;

	@Column(name = "id_medio_pago", nullable = false)
	private Long idMedioPago;

	@Column(name = "monto", nullable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	@Column(name = "referencia", length = 30)
	private String referencia;

	@Column(name = "id_transaccion")
	private Long idTransaccion;

	protected VentaPago() {
	}

	VentaPago(Venta venta, Long idMedioPago, BigDecimal monto, Long idTransaccion) {
		this.venta = venta;
		this.idMedioPago = idMedioPago;
		this.monto = monto;
		this.idTransaccion = idTransaccion;
	}

	public Long getIdVenta() {
		return venta.getIdVenta();
	}

}
