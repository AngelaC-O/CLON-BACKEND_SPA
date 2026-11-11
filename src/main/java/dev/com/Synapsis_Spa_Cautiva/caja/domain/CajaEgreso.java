package dev.com.Synapsis_Spa_Cautiva.caja.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** TRG_CAJA_EGRESO_REGLAS fija el usuario de PKG_SESION y la fecha de Oracle al insertar. */
@Entity
@Table(name = "CAJA_EGRESO")
@Getter
public class CajaEgreso {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_egreso")
	private Long idEgreso;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_caja", nullable = false)
	private Caja caja;

	@Column(name = "concepto", nullable = false, length = 120)
	private String concepto;

	@Column(name = "monto", nullable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	protected CajaEgreso() {
	}

	CajaEgreso(Caja caja, String concepto, BigDecimal monto, Long idUsuario) {
		this.caja = caja;
		this.concepto = concepto;
		this.monto = monto;
		this.fecha = LocalDateTime.now();
		this.idUsuario = idUsuario;
	}

	public Long getIdCaja() {
		return caja.getIdCaja();
	}

}
