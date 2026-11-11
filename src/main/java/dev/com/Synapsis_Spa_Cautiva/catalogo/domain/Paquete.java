package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.math.BigDecimal;
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

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Paquete o membresía por sesiones (CUS59, RF84). Solo la Administradora lo crea (TRG_ADM_PAQUETE).
 */
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

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PaqueteServicio> servicios = new ArrayList<>();

	protected Paquete() {
	}

	/** CK_PAQUETE_SESIONES, CK_PAQUETE_PRECIO y CK_PAQUETE_VIGENCIA, para responder antes de llegar a Oracle. */
	public Paquete(String nombre, TipoPaquete tipo, Integer numSesiones, BigDecimal precio, Integer vigenciaDias) {
		if (numSesiones == null || numSesiones < 1) {
			throw new ReglaNegocioException("RF84", "El paquete tiene al menos una sesión.");
		}
		if (precio == null || precio.signum() < 0) {
			throw new ReglaNegocioException("RF84", "El precio del paquete es mayor o igual a 0.");
		}
		if (vigenciaDias != null && vigenciaDias <= 0) {
			throw new ReglaNegocioException("RF84", "La vigencia del paquete, si la tiene, es de al menos un día.");
		}
		this.nombre = nombre;
		this.tipo = tipo;
		this.numSesiones = numSesiones;
		this.precio = precio;
		this.vigenciaDias = vigenciaDias;
		this.estado = EstadoPaquete.ACTIVO;
	}

	/** RF84, RN36: un servicio se incluye una sola vez (PK_PAQUETE_SERVICIO). */
	public void incluirServicio(Long idServicio) {
		boolean yaIncluido = servicios.stream().anyMatch(servicio -> servicio.getIdServicio().equals(idServicio));
		if (!yaIncluido) {
			servicios.add(new PaqueteServicio(this, idServicio));
		}
	}

	public List<PaqueteServicio> getServicios() {
		return Collections.unmodifiableList(servicios);
	}

}
