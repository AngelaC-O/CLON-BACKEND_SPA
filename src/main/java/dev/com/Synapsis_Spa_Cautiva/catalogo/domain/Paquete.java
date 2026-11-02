package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.math.BigDecimal;

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
@Table(name = "PAQUETE")
@Getter
public class Paquete {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_paquete")
	private Long idPaquete;

	@Column(name = "nombre", nullable = false, length = 80)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10)
	private TipoPaquete tipo;

	@Column(name = "num_sesiones", nullable = false)
	private Integer numSesiones;

	@Column(name = "precio", nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	@Column(name = "vigencia_dias")
	private Integer vigenciaDias;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoPaquete estado;

	protected Paquete() {
	}

}
