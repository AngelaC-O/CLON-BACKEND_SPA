package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "ROL")
@Getter
public class Rol {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_rol")
	private Long idRol;

	@Column(name = "nombre", nullable = false, length = 30)
	private String nombre;

	@Column(name = "descripcion", length = 150)
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoRol estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RolPermiso> permisos = new ArrayList<>();

	protected Rol() {
	}

	public void actualizar(String descripcion, EstadoRol estado) {
		this.descripcion = descripcion;
		this.estado = estado;
	}

	public void concederPermiso(Long idPermiso) {
		boolean yaConcedido = permisos.stream().anyMatch(permiso -> permiso.getIdPermiso().equals(idPermiso));
		if (!yaConcedido) {
			permisos.add(new RolPermiso(this, idPermiso));
		}
	}

	public List<RolPermiso> getPermisos() {
		return Collections.unmodifiableList(permisos);
	}

}
