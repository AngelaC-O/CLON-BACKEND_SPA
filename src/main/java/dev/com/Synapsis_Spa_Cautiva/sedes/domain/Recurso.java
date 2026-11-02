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

	@Column(name = "minutos_limpieza", nullable = false)
	private Integer minutosLimpieza;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 15)
	private EstadoRecurso estado;

	protected Recurso() {
	}

	public Recurso(Long idSede, String nombre, TipoRecurso tipo, Integer minutosLimpieza) {
		this.idSede = idSede;
		this.nombre = nombre;
		this.tipo = tipo;
		this.minutosLimpieza = minutosLimpieza;
		this.estado = EstadoRecurso.DISPONIBLE;
	}

}
