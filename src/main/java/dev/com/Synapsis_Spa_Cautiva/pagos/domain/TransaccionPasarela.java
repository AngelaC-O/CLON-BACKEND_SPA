package dev.com.Synapsis_Spa_Cautiva.pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
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

@Entity
@Table(name = "TRANSACCION_PASARELA")
@DynamicUpdate
@Getter
public class TransaccionPasarela {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_transaccion")
	private Long idTransaccion;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Column(name = "id_medio_pago", nullable = false)
	private Long idMedioPago;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_operacion", nullable = false, length = 10)
	private TipoOperacion tipoOperacion;

	@Enumerated(EnumType.STRING)
	@Column(name = "canal", nullable = false, length = 10)
	private CanalTransaccionPasarela canal;

	@Column(name = "id_cita")
	private Long idCita;

	@Column(name = "id_transaccion_origen")
	private Long idTransaccionOrigen;

	@Column(name = "id_nota_credito")
	private Long idNotaCredito;

	@Column(name = "codigo_operacion", length = 60)
	private String codigoOperacion;

	@Column(name = "enlace_pago", length = 300)
	private String enlacePago;

	@Column(name = "fecha_expiracion")
	private LocalDateTime fechaExpiracion;

	@Column(name = "monto", nullable = false, precision = 10, scale = 2)
	private BigDecimal monto;

	@Column(name = "comision", precision = 10, scale = 2)
	private BigDecimal comision;

	@Column(name = "fecha_abono")
	private LocalDate fechaAbono;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoTransaccion estado;

	@Column(name = "mensaje_respuesta", length = 200)
	private String mensajeRespuesta;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "fecha_hora", updatable = false, nullable = false)
	private LocalDateTime fechaHora;

	@Column(name = "fecha_respuesta")
	private LocalDateTime fechaRespuesta;

	protected TransaccionPasarela() {
	}

	public TransaccionPasarela(Long idSede, Long idMedioPago, TipoOperacion tipoOperacion, CanalTransaccionPasarela canal, BigDecimal monto,
			Long idCita, Long idUsuario) {
		this.idSede = idSede;
		this.idMedioPago = idMedioPago;
		this.tipoOperacion = tipoOperacion;
		this.canal = canal;
		this.monto = monto;
		this.idCita = idCita;
		this.idUsuario = idUsuario;
		this.estado = EstadoTransaccion.PENDIENTE;
		this.fechaHora = LocalDateTime.now();
	}

	public void registrarEnlace(String enlacePago, LocalDateTime fechaExpiracion) {
		this.enlacePago = enlacePago;
		this.fechaExpiracion = fechaExpiracion;
	}

	/** RN42: una notificación repetida de la pasarela no cambia nada (webhook idempotente). */
	public void aprobar(String codigoOperacion) {
		if (estado == EstadoTransaccion.APROBADA && codigoOperacion.equals(this.codigoOperacion)) {
			return;
		}
		if (estado != EstadoTransaccion.PENDIENTE) {
			throw new ReglaNegocioException("RN42", "La transacción está " + estado + " y no se puede aprobar.");
		}
		this.codigoOperacion = codigoOperacion;
		this.estado = EstadoTransaccion.APROBADA;
		this.fechaRespuesta = LocalDateTime.now();
	}

}
