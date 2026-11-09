package dev.com.Synapsis_Spa_Cautiva.agenda.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Composición de Atencion (CUS12, RF16). El producto es de inventario: agenda solo guarda su id.
 */
@Entity
@Table(name = "RECOMENDACION_PRODUCTO")
@Getter
public class RecomendacionProducto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_recomendacion")
	private Long idRecomendacion;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_atencion", nullable = false)
	private Atencion atencion;

	@Column(name = "id_producto", nullable = false)
	private Long idProducto;

	@Column(name = "observacion", length = 200)
	private String observacion;

	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	protected RecomendacionProducto() {
	}

	RecomendacionProducto(Atencion atencion, Long idProducto, String observacion) {
		this.atencion = atencion;
		this.idProducto = idProducto;
		this.observacion = observacion;
		this.fecha = LocalDateTime.now();
	}

	public Long getIdAtencion() {
		return atencion.getIdAtencion();
	}

}
