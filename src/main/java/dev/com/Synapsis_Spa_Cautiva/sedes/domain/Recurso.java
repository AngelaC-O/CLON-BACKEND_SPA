package dev.com.Synapsis_Spa_Cautiva.sedes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

/** [It. 2.1 · v5.2] Sin tiempo de limpieza: el recurso queda libre en cuanto termina el servicio (RN03, RF85). */
@Entity
@Table(name = "RECURSO")
@Getter
public class Recurso {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_recurso")
	private Long idRecurso;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Column(name = "nombre", nullable = false, length = 60)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 15)
	private TipoRecurso tipo;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 15)
	private EstadoRecurso estado;

	protected Recurso() {
	}

	/** RF85: el recurso se registra con su tipo y su sede, DISPONIBLE. */
	public Recurso(Long idSede, String nombre, TipoRecurso tipo) {
		this.idSede = idSede;
		this.nombre = nombre;
		this.tipo = tipo;
		this.estado = EstadoRecurso.DISPONIBLE;
	}

}
