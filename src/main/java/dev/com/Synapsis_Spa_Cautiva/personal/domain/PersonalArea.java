package dev.com.Synapsis_Spa_Cautiva.personal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "PERSONAL_AREA")
@Getter
public class PersonalArea {

	@EmbeddedId
	private PersonalAreaId id = new PersonalAreaId();

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("idPersonal")
	@JoinColumn(name = "id_personal")
	private Personal personal;

	protected PersonalArea() {
	}

	public Long getIdPersonal() {
		return id.getIdPersonal();
	}

	public Long getIdArea() {
		return id.getIdArea();
	}

}
