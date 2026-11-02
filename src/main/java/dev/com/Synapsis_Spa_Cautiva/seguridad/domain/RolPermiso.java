package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

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
@Table(name = "ROL_PERMISO")
@Getter
public class RolPermiso {

	@EmbeddedId
	private RolPermisoId id = new RolPermisoId();

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("idRol")
	@JoinColumn(name = "id_rol")
	private Rol rol;

	protected RolPermiso() {
	}

	RolPermiso(Rol rol, Long idPermiso) {
		this.rol = rol;
		this.id = new RolPermisoId(idPermiso);
	}

	public Long getIdRol() {
		return id.getIdRol();
	}

	public Long getIdPermiso() {
		return id.getIdPermiso();
	}

}
