package dev.com.Synapsis_Spa_Cautiva.agenda.domain;

import java.math.BigDecimal;
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

@Entity
@Table(name = "SALDO_FAVOR_MOV")
@Getter
public class SaldoFavorMov {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_movimiento")
	private Long idMovimiento;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@Column(name = "id_cita")
	private Long idCita;

	@Column(name = "id_venta")
	private Long idVenta;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10)
	private TipoSaldoFavorMov tipo;

	@Column(name = "monto", nullable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	@Column(name = "motivo", length = 150)
	private String motivo;

	protected SaldoFavorMov() {
	}

	/** Oracle bloquea a la clienta y rechaza el uso si el saldo no alcanza (TRG_SALDO_FAVOR_REGLAS). */
	SaldoFavorMov(Cliente cliente, Long idVenta, BigDecimal monto) {
		this.cliente = cliente;
		this.idVenta = idVenta;
		this.tipo = TipoSaldoFavorMov.USO;
		this.monto = monto;
		this.fecha = LocalDateTime.now();
	}

	public Long getIdCliente() {
		return cliente.getIdCliente();
	}

}
