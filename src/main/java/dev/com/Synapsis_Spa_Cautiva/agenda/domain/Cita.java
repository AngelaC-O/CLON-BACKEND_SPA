package dev.com.Synapsis_Spa_Cautiva.agenda.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.Getter;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.http.HttpStatus;

/**
 * fechaHoraFin la calcula TRG_CITA_REGLAS: inicio + duración ([It. 2.1 · v5.2] sin limpieza, RN03). El mismo trigger fija
 * quién registró la cita y cuándo (idUsuario y fechaRegistro), así que Hibernate los vuelve a leer al insertar.
 */
@Entity
@Table(name = "CITA")
@DynamicUpdate
@Getter
public class Cita {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cita")
	private Long idCita;

	@Column(name = "id_cliente", nullable = false)
	private Long idCliente;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Column(name = "id_personal", nullable = false)
	private Long idPersonal;

	@Column(name = "id_recurso", nullable = false)
	private Long idRecurso;

	@Column(name = "id_variante", nullable = false)
	private Long idVariante;

	@Column(name = "fecha_hora_inicio", nullable = false)
	private LocalDateTime fechaHoraInicio;

	@Column(name = "duracion_min", nullable = false)
	private Integer duracionMin;

	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "fecha_hora_fin", insertable = false, updatable = false)
	private LocalDateTime fechaHoraFin;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 15)
	private EstadoCita estado;

	@Column(name = "adelanto_monto", nullable = false, precision = 10, scale = 2)
	private BigDecimal adelantoMonto;

	@Column(name = "id_medio_pago")
	private Long idMedioPago;

	@Column(name = "fecha_adelanto")
	private LocalDateTime fechaAdelanto;

	@Column(name = "id_caja")
	private Long idCaja;

	@Column(name = "id_cita_origen")
	private Long idCitaOrigen;

	@Column(name = "motivo_cancelacion", length = 200)
	private String motivoCancelacion;

	@Column(name = "fecha_recordatorio")
	private LocalDateTime fechaRecordatorio;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "fecha_registro", updatable = false, nullable = false)
	private LocalDateTime fechaRegistro;

	protected Cita() {
	}

	public Cita(Long idCliente, Long idSede, Long idPersonal, Long idRecurso, Long idVariante, LocalDateTime fechaHoraInicio,
			Integer duracionMin, Long idUsuario) {
		this.idCliente = idCliente;
		this.idSede = idSede;
		this.idPersonal = idPersonal;
		this.idRecurso = idRecurso;
		this.idVariante = idVariante;
		this.fechaHoraInicio = fechaHoraInicio;
		this.duracionMin = duracionMin;
		this.idUsuario = idUsuario;
		this.estado = EstadoCita.PENDIENTE;
		this.adelantoMonto = BigDecimal.ZERO;
		this.fechaRegistro = LocalDateTime.now();
	}

	/** Cita nueva de una reprogramación: misma clienta, sede, servicio y duración, con el adelanto de la original (RN05). */
	private Cita(Cita origen, LocalDateTime fechaHoraInicio, Long idPersonal, Long idRecurso) {
		this(origen.idCliente, origen.idSede, idPersonal, idRecurso, origen.idVariante, fechaHoraInicio, origen.duracionMin,
				origen.idUsuario);
		this.adelantoMonto = origen.adelantoMonto;
		this.idMedioPago = origen.idMedioPago;
		this.fechaAdelanto = origen.fechaAdelanto;
		this.idCaja = origen.idCaja;
		this.idCitaOrigen = origen.idCita;
	}

	/**
	 * RF12, RN05: la cita queda REPROGRAMADA y nace otra PENDIENTE con id_cita_origen, que hereda el adelanto completo.
	 * La fecha futura (RN49) y el cruce (RN02) los valida el servicio antes, como al agendar.
	 */
	public Cita reprogramar(LocalDateTime fechaHoraInicio, Long idPersonal, Long idRecurso) {
		exigirPendiente("RF12", "reprograma");
		this.estado = EstadoCita.REPROGRAMADA;
		return new Cita(this, fechaHoraInicio, idPersonal, idRecurso);
	}

	/**
	 * RF12, RN05: se cancela con motivo, también cuando la clienta no se presenta (motivo «No se presentó»: CITA no tiene
	 * estado NO_ASISTIO). El adelanto pasa a saldo a favor con Cliente.abonarSaldo.
	 */
	public void cancelar(String motivoCancelacion) {
		exigirPendiente("RF12", "cancela");
		if (motivoCancelacion == null || motivoCancelacion.isBlank()) {
			throw new ReglaNegocioException("RF12", "Indique el motivo de la cancelación.");
		}
		this.motivoCancelacion = motivoCancelacion.trim();
		this.estado = EstadoCita.CANCELADA;
	}

	/** RN50: solo la especialista asignada a la cita, o la Administradora, inicia y finaliza su atención (403). */
	public void verificarEspecialista(Long idPersonalSesion, boolean esAdministradora) {
		if (!esAdministradora && !idPersonal.equals(idPersonalSesion)) {
			throw new ReglaNegocioException("RN50", "Solo la especialista asignada a la cita o la Administradora la atiende.",
					HttpStatus.FORBIDDEN);
		}
	}

	public void registrarAdelanto(BigDecimal adelantoMonto, Long idMedioPago, LocalDateTime fechaAdelanto, Long idCaja) {
		if (estado != EstadoCita.PENDIENTE) {
			throw new ReglaNegocioException("RN04", "El adelanto se registra en una cita PENDIENTE; esta está " + estado + ".");
		}
		if (adelantoMonto == null || adelantoMonto.signum() <= 0) {
			throw new ReglaNegocioException("RN04", "El adelanto debe ser mayor que 0.");
		}
		this.adelantoMonto = adelantoMonto;
		this.idMedioPago = idMedioPago;
		this.fechaAdelanto = fechaAdelanto;
		this.idCaja = idCaja;
	}

	public void iniciarAtencion(Integer tiempoDeclaradoMin) {
		if (estado != EstadoCita.PENDIENTE) {
			throw new ReglaNegocioException("RF13", "Solo se atiende una cita PENDIENTE; esta está " + estado + ".");
		}
		this.duracionMin = tiempoDeclaradoMin;
		this.estado = EstadoCita.EN_ATENCION;
	}

	public void finalizar() {
		if (estado != EstadoCita.EN_ATENCION) {
			throw new ReglaNegocioException("RF13", "Solo se finaliza una cita EN_ATENCION; esta está " + estado + ".");
		}
		this.estado = EstadoCita.FINALIZADA;
	}

	private void exigirPendiente(String regla, String accion) {
		if (estado != EstadoCita.PENDIENTE) {
			throw new ReglaNegocioException(regla, "Solo se " + accion + " una cita PENDIENTE; esta está " + estado + ".");
		}
	}

}
