package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/** PK compuesta de ROL_PERMISO. */
@Embeddable
@Getter
public class RolPermisoId implements Serializable {

	@Column(name = "id_rol")
	private Long idRol;

	@Column(name = "id_permiso")
	private Long idPermiso;

	protected RolPermisoId() {
	}

	RolPermisoId(Long idPermiso) {
		this.idPermiso = idPermiso;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof RolPermisoId id && Objects.equals(idRol, id.idRol) && Objects.equals(idPermiso, id.idPermiso);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idRol, idPermiso);
	}

}
