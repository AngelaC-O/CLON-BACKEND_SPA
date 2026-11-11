package dev.com.Synapsis_Spa_Cautiva.caja.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * TRG_CAJA_REGLAS fija quién abre, cierra, revisa y reabre la caja, y cuándo, con PKG_SESION y SYSTIMESTAMP; al reabrir
 * limpia el cierre y la revisión (RN30, v5.1). Hibernate vuelve a leer esas columnas al guardar.
 */
@Entity
@Table(name = "CAJA")
@DynamicUpdate
@Getter
public class Caja {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_caja")
	private Long idCaja;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "id_usuario_apertura", nullable = false)
	private Long idUsuarioApertura;

	@Column(name = "fecha_apertura", nullable = false)
	private LocalDateTime fechaApertura;

	@Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
	private BigDecimal montoInicial;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "id_usuario_cierre")
	private Long idUsuarioCierre;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "fecha_cierre")
	private LocalDateTime fechaCierre;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "id_usuario_revisa")
	private Long idUsuarioRevisa;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "fecha_revision")
	private LocalDateTime fechaRevision;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoCaja estado;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "id_usuario_reapertura")
	private Long idUsuarioReapertura;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "fecha_reapertura")
	private LocalDateTime fechaReapertura;

	@Column(name = "motivo_reapertura", length = 300)
	private String motivoReapertura;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "caja", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CajaEgreso> egresos = new ArrayList<>();

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "caja", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CajaArqueo> arqueos = new ArrayList<>();

	protected Caja() {
	}

	public Caja(Long idSede, Long idUsuarioApertura, BigDecimal montoInicial) {
		this.idSede = idSede;
		this.idUsuarioApertura = idUsuarioApertura;
		this.montoInicial = montoInicial;
		this.fechaApertura = LocalDateTime.now();
		this.estado = EstadoCaja.ABIERTA;
	}

	public void registrarEgreso(String concepto, BigDecimal monto, Long idUsuario) {
		exigirAbierta();
		egresos.add(new CajaEgreso(this, concepto, monto, idUsuario));
	}

	/** Lo esperado y la diferencia los calcula Oracle (TRG_CAJA_ARQUEO_REGLAS); un medio ya contado se corrige. */
	public void registrarArqueo(Long idMedioPago, BigDecimal montoContado, String observacion) {
		exigirAbierta();
		arqueos.stream()
			.filter(arqueo -> arqueo.getIdMedioPago().equals(idMedioPago))
			.findFirst()
			.ifPresentOrElse(arqueo -> arqueo.corregir(montoContado, observacion),
					() -> arqueos.add(new CajaArqueo(this, idMedioPago, montoContado, observacion)));
	}

	/** RN30: cierre con arqueo por medio de pago y motivo para cada diferencia ya calculada. */
	public void cerrar(Long idUsuarioCierre) {
		exigirAbierta();
		if (arqueos.isEmpty()) {
			throw new ReglaNegocioException("RN30", "Registre el arqueo por medio de pago antes de cerrar la caja.");
		}
		if (arqueos.stream().anyMatch(CajaArqueo::faltaObservacion)) {
			throw new ReglaNegocioException("RN30", "Toda diferencia del arqueo lleva una observación.");
		}
		this.idUsuarioCierre = idUsuarioCierre;
		this.fechaCierre = LocalDateTime.now();
		this.estado = EstadoCaja.CERRADA;
	}

	/** RF67: la Administradora aprueba el cierre con diferencia; la caja sigue CERRADA y queda revisada. */
	public void aprobarRevision(Long idUsuarioRevisa, LocalDateTime fechaRevision) {
		if (estado != EstadoCaja.CERRADA) {
			throw new ReglaNegocioException("RF67", "Solo se revisa una caja CERRADA; esta está " + estado + ".");
		}
		if (this.idUsuarioRevisa != null) {
			throw new ReglaNegocioException("RF67", "El cierre de esta caja ya se aprobó.");
		}
		if (arqueos.stream().noneMatch(CajaArqueo::tieneDiferencia)) {
			throw new ReglaNegocioException("RF67", "El cierre no tiene diferencias que aprobar.");
		}
		this.idUsuarioRevisa = idUsuarioRevisa;
		this.fechaRevision = fechaRevision;
	}

	/**
	 * RF66, RN30: la Administradora reabre la caja CERRADA del día con un motivo, para registrar una venta tardía; luego se
	 * vuelve a arquear y cerrar. Que no haya otra caja ABIERTA en la sede lo exige UQ_CAJA_ABIERTA_SEDE (409).
	 */
	public void reabrir(Long idUsuarioReapertura, String motivoReapertura) {
		if (estado != EstadoCaja.CERRADA) {
			throw new ReglaNegocioException("RN30", "Solo se reabre una caja CERRADA; esta está " + estado + ".");
		}
		if (!fechaApertura.toLocalDate().equals(LocalDate.now())) {
			throw new ReglaNegocioException("RN30", "Solo se reabre la caja del día.");
		}
		if (motivoReapertura == null || motivoReapertura.isBlank()) {
			throw new ReglaNegocioException("RN30", "Indique el motivo de la reapertura.");
		}
		this.estado = EstadoCaja.ABIERTA;
		this.idUsuarioReapertura = idUsuarioReapertura;
		this.fechaReapertura = LocalDateTime.now();
		this.motivoReapertura = motivoReapertura.trim();
		this.idUsuarioCierre = null;
		this.fechaCierre = null;
		this.idUsuarioRevisa = null;
		this.fechaRevision = null;
	}

	private void exigirAbierta() {
		if (estado != EstadoCaja.ABIERTA) {
			throw new ReglaNegocioException("RN30", "La caja está " + estado + " y no se modifica.");
		}
	}

	public List<CajaEgreso> getEgresos() {
		return Collections.unmodifiableList(egresos);
	}

	public List<CajaArqueo> getArqueos() {
		return Collections.unmodifiableList(arqueos);
	}

}
