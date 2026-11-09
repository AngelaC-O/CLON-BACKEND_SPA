package dev.com.Synapsis_Spa_Cautiva.caja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.SiNoConverter;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Composición de Conciliacion (CUS47, RN31): una venta, un comprobante de SUNAT o una transacción de la pasarela comparados
 * (CK_CONC_ITEM_ORIGEN: al menos uno de los tres). La venta y la transacción son de otros módulos y van por id.
 */
@Entity
@Table(name = "CONCILIACION_ITEM")
@Getter
public class ConciliacionItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_item")
	private Long idItem;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_conciliacion", nullable = false)
	private Conciliacion conciliacion;

	@Column(name = "id_venta")
	private Long idVenta;

	@Column(name = "referencia_sunat", length = 20)
	private String referenciaSunat;

	@Column(name = "id_transaccion")
	private Long idTransaccion;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "aceptado_sunat", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean aceptadoSunat;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "pago_respaldado", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean pagoRespaldado;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 15)
	private EstadoConciliacionItem estado;

	@Column(name = "justificacion", length = 300)
	private String justificacion;

	protected ConciliacionItem() {
	}

	/** Coincide si SUNAT aceptó el comprobante y el pago está respaldado; si no, queda SIN_JUSTIFICAR. */
	ConciliacionItem(Conciliacion conciliacion, Long idVenta, String referenciaSunat, Long idTransaccion, boolean aceptadoSunat,
			boolean pagoRespaldado) {
		if (idVenta == null && referenciaSunat == null && idTransaccion == null) {
			throw new ReglaNegocioException("RN31", "Cada diferencia nace de una venta, de SUNAT o de la pasarela.");
		}
		this.conciliacion = conciliacion;
		this.idVenta = idVenta;
		this.referenciaSunat = referenciaSunat;
		this.idTransaccion = idTransaccion;
		this.aceptadoSunat = aceptadoSunat;
		this.pagoRespaldado = pagoRespaldado;
		this.estado = aceptadoSunat && pagoRespaldado ? EstadoConciliacionItem.COINCIDE : EstadoConciliacionItem.SIN_JUSTIFICAR;
	}

	/** RF75: la diferencia queda JUSTIFICADA con su explicación (CK_CONC_ITEM_JUSTIFICADA). */
	void justificar(String texto) {
		exigirDiferencia();
		if (texto == null || texto.isBlank()) {
			throw new ReglaNegocioException("RF75", "Escriba la justificación de la diferencia.");
		}
		this.justificacion = texto.trim();
		this.estado = EstadoConciliacionItem.JUSTIFICADA;
	}

	/** RF75: o se EXCLUYE de la conciliación, con el motivo si se indica (por ejemplo, una venta anulada). */
	void excluir(String texto) {
		exigirDiferencia();
		this.justificacion = texto == null || texto.isBlank() ? null : texto.trim();
		this.estado = EstadoConciliacionItem.EXCLUIDA;
	}

	boolean sinJustificar() {
		return estado == EstadoConciliacionItem.SIN_JUSTIFICAR;
	}

	private void exigirDiferencia() {
		if (estado == EstadoConciliacionItem.COINCIDE) {
			throw new ReglaNegocioException("RF75", "El ítem coincide: no hay diferencia que justificar.");
		}
	}

	public Long getIdConciliacion() {
		return conciliacion.getIdConciliacion();
	}

}
