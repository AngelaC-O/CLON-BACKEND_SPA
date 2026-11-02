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
@Table(name = "SEDE")
@Getter
public class Sede {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_sede")
	private Long idSede;

	@Column(name = "nombre", nullable = false, length = 80)
	private String nombre;

	@Column(name = "direccion", nullable = false, length = 150)
	private String direccion;

	@Column(name = "horario_apertura", nullable = false, length = 5)
	private String horarioApertura;

	@Column(name = "horario_cierre", nullable = false, length = 5)
	private String horarioCierre;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoSede estado;

	protected Sede() {
	}

}
