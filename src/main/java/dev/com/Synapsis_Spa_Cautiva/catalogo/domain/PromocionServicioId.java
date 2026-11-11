package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/** PK compuesta de PROMOCION_SERVICIO. */
@Embeddable
@Getter
public class PromocionServicioId implements Serializable {

	@Column(name = "id_promocion")
	private Long idPromocion;

	@Column(name = "id_servicio")
	private Long idServicio;

	protected PromocionServicioId() {
	}

	PromocionServicioId(Long idServicio) {
		this.idServicio = idServicio;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof PromocionServicioId id && Objects.equals(idPromocion, id.idPromocion)
				&& Objects.equals(idServicio, id.idServicio);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idPromocion, idServicio);
	}

}
