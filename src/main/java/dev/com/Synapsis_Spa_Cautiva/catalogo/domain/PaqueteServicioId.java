package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/** PK compuesta de PAQUETE_SERVICIO. */
@Embeddable
@Getter
public class PaqueteServicioId implements Serializable {

	@Column(name = "id_paquete")
	private Long idPaquete;

	@Column(name = "id_servicio")
	private Long idServicio;

	protected PaqueteServicioId() {
	}

	PaqueteServicioId(Long idServicio) {
		this.idServicio = idServicio;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof PaqueteServicioId id && Objects.equals(idPaquete, id.idPaquete) && Objects.equals(idServicio, id.idServicio);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idPaquete, idServicio);
	}

}
