package dev.com.Synapsis_Spa_Cautiva.sedes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.Getter;

@Entity
@Table(name = "RECURSO")
@Getter
public class Recurso {

	/**
	 * Tope de CK_RECURSO_MINUTOS_LIMPIEZA (0 a 10, modelo v5.1). El tope del negocio es LIMPIEZA_MAX_MIN de PARAMETRO, que
	 * revisa ServicioRecurso; este solo evita mandar a Oracle un valor que el CHECK rechazaría.
	 */
	private static final int LIMPIEZA_MAXIMA_DEL_ESQUEMA = 10;

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

	/** RN03: después de cada servicio el recurso queda bloqueado por su limpieza, de 0 a 10 minutos. */
	public Recurso(Long idSede, String nombre, TipoRecurso tipo, Integer minutosLimpieza) {
		if (minutosLimpieza == null || minutosLimpieza < 0 || minutosLimpieza > LIMPIEZA_MAXIMA_DEL_ESQUEMA) {
			throw new ReglaNegocioException("RN03", "El tiempo de limpieza va de 0 a " + LIMPIEZA_MAXIMA_DEL_ESQUEMA + " minutos.");
		}
		this.idSede = idSede;
		this.nombre = nombre;
		this.tipo = tipo;
		this.minutosLimpieza = minutosLimpieza;
		this.estado = EstadoRecurso.DISPONIBLE;
	}

}
