package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

/** Composición de Paquete: servicios que incluye el paquete o la membresía (CUS59, RF84, RN36). */
@Entity
@Table(name = "PAQUETE_SERVICIO")
@Getter
public class PaqueteServicio {

	@EmbeddedId
	private PaqueteServicioId id = new PaqueteServicioId();

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("idPaquete")
	@JoinColumn(name = "id_paquete")
	private Paquete paquete;

	protected PaqueteServicio() {
	}

	PaqueteServicio(Paquete paquete, Long idServicio) {
		this.paquete = paquete;
		this.id = new PaqueteServicioId(idServicio);
	}

	public Long getIdPaquete() {
		return id.getIdPaquete();
	}

	public Long getIdServicio() {
		return id.getIdServicio();
	}

}
