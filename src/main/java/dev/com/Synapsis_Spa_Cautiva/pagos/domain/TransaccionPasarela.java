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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

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

	/**
	 * [It. 2.1 · v5.2 · D-22] Venta cuyo pago se cobra (FK_TRANS_PAS_VENTA). Nula en los adelantos de cita y en las devoluciones
	 * (CK_TRANS_PAS_VENTA). No cambia: TRG_TRANS_PAS_REGLAS responde -20072.
	 */
	@Column(name = "id_venta", updatable = false)
	private Long idVenta;

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

	/** TRG_TRANS_PAS_REGLAS pone el usuario de PKG_SESION y la fecha de Oracle al insertar. */
	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Generated(event = EventType.INSERT, writable = true)
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

	/**
	 * RF106, RN44: devolución de un cobro APROBADO por el mismo medio (CUS24). Con tarjeta va por la API de la pasarela;
	 * Yape y Plin los devuelve la Administradora desde la app y se registran con canal QR y su número de operación
	 * (aprobar). TRG_TRANS_PAS_REGLAS exige la Administradora y que las devoluciones no superen el cobro (-20077).
	 */
	public TransaccionPasarela(Long idSede, Long idMedioPago, TipoOperacion tipoOperacion, CanalTransaccionPasarela canal, BigDecimal monto,
			Long idTransaccionOrigen, Long idNotaCredito, Long idUsuario) {
		if (tipoOperacion != TipoOperacion.DEVOLUCION || idTransaccionOrigen == null) {
			throw new ReglaNegocioException("RN44", "Una devolución se registra contra el cobro que devuelve.");
		}
		if (canal == CanalTransaccionPasarela.LINK) {
			throw new ReglaNegocioException("RN44", "El enlace de pago solo sirve para cobrar (CK_TRANS_PAS_LINK).");
		}
		if (monto == null || monto.signum() <= 0) {
			throw new ReglaNegocioException("RN44", "El monto de la devolución es mayor que 0.");
		}
		this.idSede = idSede;
		this.idMedioPago = idMedioPago;
		this.tipoOperacion = tipoOperacion;
		this.canal = canal;
		this.monto = monto;
		this.idTransaccionOrigen = idTransaccionOrigen;
		this.idNotaCredito = idNotaCredito;
		this.idUsuario = idUsuario;
		this.estado = EstadoTransaccion.PENDIENTE;
		this.fechaHora = LocalDateTime.now();
	}

	/**
	 * [It. 2.1 · D-22, RN52] El cobro es del pago de esta venta: así GET /ventas/{idVenta} lista sus cobros PENDIENTES. Solo un COBRO sin cita y
	 * antes de guardarlo (CK_TRANS_PAS_VENTA; Oracle además exige la venta ABIERTA de la misma sede, -20072). No está en el diagrama (DF-160).
	 */
	public void registrarVenta(Long idVenta) {
		if (tipoOperacion != TipoOperacion.COBRO || idCita != null || this.idTransaccion != null) {
			throw new ReglaNegocioException("RN52", "Solo un cobro nuevo que no es de una cita se registra para una venta.");
		}
		this.idVenta = idVenta;
	}

	public void registrarEnlace(String enlacePago, LocalDateTime fechaExpiracion) {
		this.enlacePago = enlacePago;
		this.fechaExpiracion = fechaExpiracion;
	}

	/** El webhook busca la transacción por este código: se guarda al crear el enlace (D-05). */
	public void registrarEnlace(String enlacePago, LocalDateTime fechaExpiracion, String codigoOperacion) {
		registrarEnlace(enlacePago, fechaExpiracion);
		registrarCodigoOperacion(codigoOperacion);
	}

	/** Un cobro PENDIENTE guarda el código con el que llegará el webhook. */
	public void registrarCodigoOperacion(String codigoOperacion) {
		this.codigoOperacion = codigoOperacion;
	}

	public void rechazar() {
		if (estado != EstadoTransaccion.PENDIENTE) {
			throw new ReglaNegocioException("RN42", "La transacción está " + estado + " y no se puede rechazar.");
		}
		this.estado = EstadoTransaccion.RECHAZADA;
		this.fechaRespuesta = LocalDateTime.now();
	}

	/**
	 * [It. 2 · CUS47, RF105] La comisión y la fecha de abono del reporte de liquidación de Izipay; TRG_TRANS_PAS_REGLAS deja cambiarlas. Solo de un
	 * cobro APROBADO. No está en el diagrama: lo pide PagosApi.registrarLiquidacion (DF-125).
	 */
	public void registrarLiquidacion(BigDecimal comision, LocalDate fechaAbono) {
		if (estado != EstadoTransaccion.APROBADA || tipoOperacion != TipoOperacion.COBRO) {
			throw new ReglaNegocioException("RF105", "Solo se liquida un cobro APROBADO; la transacción " + idTransaccion + " está " + estado + ".");
		}
		if (comision != null && comision.signum() < 0) {
			throw new ReglaNegocioException("RF105", "La comisión de la pasarela no es negativa.");
		}
		this.comision = comision;
		this.fechaAbono = fechaAbono;
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
