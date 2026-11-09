package dev.com.Synapsis_Spa_Cautiva.caja.domain;

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

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * montoEsperado y diferencia los calcula TRG_CAJA_ARQUEO_REGLAS con V_CAJA_ESPERADO.
 */
@Entity
@Table(name = "CAJA_ARQUEO")
@DynamicUpdate
@Getter
public class CajaArqueo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_arqueo")
	private Long idArqueo;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_caja", nullable = false)
	private Caja caja;

	@Column(name = "id_medio_pago", nullable = false)
	private Long idMedioPago;

	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "monto_esperado", insertable = false, updatable = false, precision = 10, scale = 2)
	private BigDecimal montoEsperado;

	@Column(name = "monto_contado", nullable = false, precision = 10, scale = 2)
	private BigDecimal montoContado;

	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "diferencia", insertable = false, updatable = false, precision = 10, scale = 2)
	private BigDecimal diferencia;

	@Column(name = "observacion", length = 300)
	private String observacion;

	protected CajaArqueo() {
	}

	CajaArqueo(Caja caja, Long idMedioPago, BigDecimal montoContado, String observacion) {
		this.caja = caja;
		this.idMedioPago = idMedioPago;
		this.montoContado = montoContado;
		this.observacion = observacion;
	}

	void corregir(BigDecimal montoContado, String observacion) {
		this.montoContado = montoContado;
		this.observacion = observacion;
	}

	boolean faltaObservacion() {
		return tieneDiferencia() && (observacion == null || observacion.isBlank());
	}

	boolean tieneDiferencia() {
		return diferencia != null && diferencia.signum() != 0;
	}

	public Long getIdCaja() {
		return caja.getIdCaja();
	}

}
