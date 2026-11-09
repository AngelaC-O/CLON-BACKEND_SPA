package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

import java.time.LocalDateTime;
import java.util.Set;

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
 * Raíz de su propio agregado (RN32): la recepcionista pide anular una venta CERRADA (CUS22) y la Administradora la aprueba,
 * con su nota de crédito, o la rechaza con su motivo (CUS23). La venta y la nota van por id.
 * TRG_SOLICITUD_ANULACION_REGLAS fija quién y cuándo la pidió al insertar y quién y cuándo la resolvió al actualizar;
 * Hibernate vuelve a leer esas columnas al guardar. Una sola PENDIENTE por venta (UQ_SOL_ANUL_PENDIENTE).
 */
@Entity
@Table(name = "SOLICITUD_ANULACION")
@DynamicUpdate
@Getter
public class SolicitudAnulacion {

	/** Catálogo 09 de SUNAT: anulación de la operación, error en el RUC y devolución total (CK_SOL_ANUL_COD_MOTIVO). */
	private static final Set<String> CODIGOS_MOTIVO = Set.of("01", "02", "06");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_solicitud")
	private Long idSolicitud;

	@Column(name = "id_venta", nullable = false, updatable = false)
	private Long idVenta;

	@Column(name = "motivo", nullable = false, length = 200)
	private String motivo;

	@Column(name = "codigo_motivo", nullable = false, length = 2)
	private String codigoMotivo;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_devolucion", nullable = false, length = 15)
	private FormaDevolucion formaDevolucion;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "id_usuario_solicita", nullable = false, updatable = false)
	private Long idUsuarioSolicita;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "fecha_solicitud", nullable = false, updatable = false)
	private LocalDateTime fechaSolicitud;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoSolicitudAnulacion estado;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "id_usuario_resuelve")
	private Long idUsuarioResuelve;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "fecha_resolucion")
	private LocalDateTime fechaResolucion;

	@Column(name = "motivo_rechazo", length = 300)
	private String motivoRechazo;

	@Column(name = "id_nota_credito")
	private Long idNotaCredito;

	protected SolicitudAnulacion() {
	}

	/** RF32, RN32, RF106: la venta debe estar CERRADA (lo valida el servicio y lo repite Oracle, -20101). */
	public SolicitudAnulacion(Long idVenta, String motivo, String codigoMotivo, FormaDevolucion formaDevolucion, Long idUsuarioSolicita) {
		if (motivo == null || motivo.isBlank()) {
			throw new ReglaNegocioException("RN32", "Indique el motivo de la anulación.");
		}
		if (!CODIGOS_MOTIVO.contains(codigoMotivo)) {
			throw new ReglaNegocioException("RN32", "El código de motivo es 01, 02 o 06 (catálogo 09 de SUNAT).");
		}
		if (formaDevolucion == null) {
			throw new ReglaNegocioException("RF106", "Indique cómo se devuelve el dinero si se aprueba.");
		}
		this.idVenta = idVenta;
		this.motivo = motivo.trim();
		this.codigoMotivo = codigoMotivo;
		this.formaDevolucion = formaDevolucion;
		this.idUsuarioSolicita = idUsuarioSolicita;
		this.fechaSolicitud = LocalDateTime.now();
		this.estado = EstadoSolicitudAnulacion.PENDIENTE;
	}

	/**
	 * RN32: la Administradora la aprueba (403 en Oracle si no lo es, -20003). La nota de crédito ya se emitió y su id va en el
	 * mismo UPDATE, porque una solicitud resuelta no cambia (-20102); es nulo si SUNAT había rechazado el comprobante.
	 */
	public void aprobar(Long idUsuarioResuelve, Long idNotaCredito) {
		exigirPendiente();
		this.idNotaCredito = idNotaCredito;
		resolver(EstadoSolicitudAnulacion.APROBADA, idUsuarioResuelve);
	}

	/** RN32: o la rechaza con su motivo, y la venta sigue CERRADA (CK_SOL_ANUL_RECHAZO). */
	public void rechazar(Long idUsuarioResuelve, String motivoRechazo) {
		exigirPendiente();
		if (motivoRechazo == null || motivoRechazo.isBlank()) {
			throw new ReglaNegocioException("RN32", "Indique el motivo del rechazo.");
		}
		this.motivoRechazo = motivoRechazo.trim();
		resolver(EstadoSolicitudAnulacion.RECHAZADA, idUsuarioResuelve);
	}

	private void resolver(EstadoSolicitudAnulacion estado, Long idUsuarioResuelve) {
		this.estado = estado;
		this.idUsuarioResuelve = idUsuarioResuelve;
		this.fechaResolucion = LocalDateTime.now();
	}

	private void exigirPendiente() {
		if (estado != EstadoSolicitudAnulacion.PENDIENTE) {
			throw new ReglaNegocioException("RN32", "La solicitud ya está " + estado + " y no cambia.");
		}
	}

}
