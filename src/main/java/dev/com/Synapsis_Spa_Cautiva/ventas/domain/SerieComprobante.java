package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

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

/**
 * Serie de comprobantes de la sede (CUS63, RF107). El correlativo solo lo mueve PKG_VENTA al emitir (RN47): Java lo inserta
 * en 0 y nunca lo actualiza, para no pisar el número que asignó Oracle.
 */
@Entity
@Table(name = "SERIE_COMPROBANTE")
@DynamicUpdate
@Getter
public class SerieComprobante {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_serie")
	private Long idSerie;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 12)
	private TipoComprobante tipo;

	@Column(name = "serie", nullable = false, length = 4)
	private String serie;

	@Column(name = "ultimo_numero", nullable = false, updatable = false)
	private Integer ultimoNumero;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoSerieComprobante estado;

	protected SerieComprobante() {
	}

	/** CK_SERIE_COMPR_SERIE y CK_SERIE_COMPR_SERIE_TIPO: B para boletas y sus notas, F para facturas y las suyas. */
	public SerieComprobante(Long idSede, TipoComprobante tipo, String serie) {
		String letra = tipo == TipoComprobante.BOLETA || tipo == TipoComprobante.NC_BOLETA ? "B" : "F";
		if (serie == null || !serie.matches("[BF][A-Z0-9]{3}") || !serie.startsWith(letra)) {
			throw new ReglaNegocioException("RF107", "La serie de " + tipo + " tiene 4 caracteres y empieza con " + letra + ".");
		}
		this.idSede = idSede;
		this.tipo = tipo;
		this.serie = serie;
		this.ultimoNumero = 0;
		this.estado = EstadoSerieComprobante.ACTIVA;
	}

}
