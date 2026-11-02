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

/**
 * fechaHoraFin la calcula TRG_CITA_REGLAS: inicio + duración + limpieza del recurso (RN03).
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

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

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

}
