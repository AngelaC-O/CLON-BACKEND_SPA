package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "PERMISO")
@Getter
public class Permiso {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_permiso")
	private Long idPermiso;

	@Column(name = "modulo", nullable = false, length = 40)
	private String modulo;

	@Column(name = "accion", nullable = false, length = 30)
	private String accion;

	@Column(name = "descripcion", length = 150)
	private String descripcion;

	protected Permiso() {
	}

	public Permiso(String modulo, String accion) {
		this.modulo = modulo;
		this.accion = accion;
	}

}
