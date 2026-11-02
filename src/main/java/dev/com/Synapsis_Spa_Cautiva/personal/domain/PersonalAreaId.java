package dev.com.Synapsis_Spa_Cautiva.personal.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/** PK compuesta de PERSONAL_AREA. */
@Embeddable
@Getter
public class PersonalAreaId implements Serializable {

	@Column(name = "id_personal")
	private Long idPersonal;

	@Column(name = "id_area")
	private Long idArea;

	protected PersonalAreaId() {
	}

	PersonalAreaId(Long idArea) {
		this.idArea = idArea;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof PersonalAreaId id && Objects.equals(idPersonal, id.idPersonal) && Objects.equals(idArea, id.idArea);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idPersonal, idArea);
	}

}
