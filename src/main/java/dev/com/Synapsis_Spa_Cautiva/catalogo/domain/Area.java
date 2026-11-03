package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

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
@Table(name = "AREA")
@Getter
public class Area {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_area")
	private Long idArea;

	@Enumerated(EnumType.STRING)
	@Column(name = "linea", nullable = false, length = 20)
	private LineaNegocio linea;

	@Column(name = "nombre", nullable = false, length = 60)
	private String nombre;

	protected Area() {
	}

	public Area(LineaNegocio linea, String nombreArea) {
		this.linea = linea;
		this.nombre = nombreArea;
	}

}
