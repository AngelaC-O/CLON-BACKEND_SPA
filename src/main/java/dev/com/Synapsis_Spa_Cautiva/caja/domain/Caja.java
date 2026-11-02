package dev.com.Synapsis_Spa_Cautiva.caja.domain;

import java.math.BigDecimal;
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

	@Column(name = "id_usuario_apertura", nullable = false)
	private Long idUsuarioApertura;

	@Column(name = "fecha_apertura", nullable = false)
	private LocalDateTime fechaApertura;

	@Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
	private BigDecimal montoInicial;

	@Column(name = "id_usuario_cierre")
	private Long idUsuarioCierre;

	@Column(name = "fecha_cierre")
	private LocalDateTime fechaCierre;

	@Column(name = "id_usuario_revisa")
	private Long idUsuarioRevisa;

	@Column(name = "fecha_revision")
	private LocalDateTime fechaRevision;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoCaja estado;

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
