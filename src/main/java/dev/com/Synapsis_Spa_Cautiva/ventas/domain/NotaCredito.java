package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.Getter;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Subtipo de COMPROBANTE (CUS23, RN12): la serie BC01 o FC01 y el número los asigna PKG_VENTA al insertar el COMPROBANTE.
 * TRG_NOTA_CREDITO_REGLAS exige que id_usuario_autoriza sea la Administradora y pone en id_usuario_solicita el usuario de
 * PKG_SESION, así que Hibernate lo vuelve a leer (DF-66). Con @DynamicUpdate, registrar la respuesta de SUNAT no repite
 * id_nota_credito, que dispara TRG_NOTA_CREDITO_HERENCIA (UPDATE OF id_nota_credito).
 */
@Entity
@Table(name = "NOTA_CREDITO")
@PrimaryKeyJoinColumn(name = "id_nota_credito")
@DynamicUpdate
@Getter
public class NotaCredito extends Comprobante {

	/** Catálogo 09 de SUNAT (CK_NOTA_CREDITO_COD_MOTIVO). */
	private static final Set<String> CODIGOS_MOTIVO = Set.of("01", "02", "06");

	@Column(name = "motivo", nullable = false, length = 200)
	private String motivo;

	@Column(name = "codigo_motivo", nullable = false, length = 2)
	private String codigoMotivo;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_devolucion", nullable = false, length = 15)
	private FormaDevolucion formaDevolucion;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "id_usuario_solicita", nullable = false)
	private Long idUsuarioSolicita;

	@Column(name = "id_usuario_autoriza", nullable = false)
	private Long idUsuarioAutoriza;

	protected NotaCredito() {
	}

	/**
	 * RF33, RN32, RN44: la nota anula la boleta (NC_BOLETA) o la factura (NC_FACTURA) de la venta CERRADA; los datos salen de
	 * la solicitud aprobada. Queda dentro del agregado de la venta, para que Venta.anular sepa que ya tiene su nota.
	 */
	public NotaCredito(Venta oVenta, TipoComprobante tipo, String motivo, String codigoMotivo, FormaDevolucion formaDevolucion,
			Long idUsuarioSolicita, Long idUsuarioAutoriza) {
		super(oVenta, tipo, null);
		if (tipo != TipoComprobante.NC_BOLETA && tipo != TipoComprobante.NC_FACTURA) {
			throw new ReglaNegocioException("RN12", "Una nota de crédito es NC_BOLETA o NC_FACTURA.");
		}
		if (motivo == null || motivo.isBlank() || !CODIGOS_MOTIVO.contains(codigoMotivo)) {
			throw new ReglaNegocioException("RN32", "La nota de crédito lleva motivo y código 01, 02 o 06 (catálogo 09 de SUNAT).");
		}
		this.motivo = motivo.trim();
		this.codigoMotivo = codigoMotivo;
		this.formaDevolucion = formaDevolucion;
		this.idUsuarioSolicita = idUsuarioSolicita;
		this.idUsuarioAutoriza = idUsuarioAutoriza;
		oVenta.agregarNotaCredito(this);
	}

}
