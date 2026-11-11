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

/** Composición de Promocion: servicios a los que aplica la promoción (CUS60, RF84). */
@Entity
@Table(name = "PROMOCION_SERVICIO")
@Getter
public class PromocionServicio {

	@EmbeddedId
	private PromocionServicioId id = new PromocionServicioId();

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("idPromocion")
	@JoinColumn(name = "id_promocion")
	private Promocion promocion;

	protected PromocionServicio() {
	}

	PromocionServicio(Promocion promocion, Long idServicio) {
		this.promocion = promocion;
		this.id = new PromocionServicioId(idServicio);
	}

	public Long getIdPromocion() {
		return id.getIdPromocion();
	}

	public Long getIdServicio() {
		return id.getIdServicio();
	}

}
