package dev.com.Synapsis_Spa_Cautiva.agenda.domain;

import java.time.LocalDateTime;
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

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "ATENCION")
@DynamicUpdate
@Getter
public class Atencion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_atencion")
	private Long idAtencion;

	@Column(name = "id_cita")
	private Long idCita;

	@Column(name = "id_cliente")
	private Long idCliente;

	@Column(name = "id_personal", nullable = false)
	private Long idPersonal;

	@Column(name = "id_recurso", nullable = false)
	private Long idRecurso;

	@Column(name = "id_variante", nullable = false)
	private Long idVariante;

	@Column(name = "id_cliente_paquete")
	private Long idClientePaquete;

	@Column(name = "nombre_cliente_paso", length = 80)
	private String nombreClientePaso;

	@Column(name = "hora_inicio", nullable = false)
	private LocalDateTime horaInicio;

	@Column(name = "hora_fin")
	private LocalDateTime horaFin;

	@Column(name = "tiempo_declarado_min", nullable = false)
	private Integer tiempoDeclaradoMin;

	@Column(name = "observaciones", length = 300)
	private String observaciones;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 15)
	private EstadoAtencion estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "atencion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RecomendacionProducto> recomendaciones = new ArrayList<>();

	protected Atencion() {
	}

	public Atencion(Long idCita, Long idCliente, Long idPersonal, Long idRecurso, Long idVariante, Integer tiempoDeclaradoMin) {
		this(idPersonal, idRecurso, idVariante, tiempoDeclaradoMin);
		this.idCita = idCita;
		this.idCliente = idCliente;
	}

	/** Atención sin cita: con clienta registrada o solo con el nombre de la clienta de paso (RN35). */
	public Atencion(Long idCliente, String nombreClientePaso, Long idPersonal, Long idRecurso, Long idVariante,
			Integer tiempoDeclaradoMin) {
		this(idPersonal, idRecurso, idVariante, tiempoDeclaradoMin);
		this.idCliente = idCliente;
		this.nombreClientePaso = nombreClientePaso;
	}

	private Atencion(Long idPersonal, Long idRecurso, Long idVariante, Integer tiempoDeclaradoMin) {
		this.idPersonal = idPersonal;
		this.idRecurso = idRecurso;
		this.idVariante = idVariante;
		this.tiempoDeclaradoMin = tiempoDeclaradoMin;
		this.horaInicio = LocalDateTime.now();
		this.estado = EstadoAtencion.EN_CURSO;
	}

	public void registrarDetalle(String observaciones) {
		this.observaciones = observaciones;
	}

	public void finalizar(LocalDateTime horaFin) {
		if (estado != EstadoAtencion.EN_CURSO) {
			throw new ReglaNegocioException("RF13", "Solo se finaliza una atención EN_CURSO; esta está " + estado + ".");
		}
		this.horaFin = horaFin;
		this.estado = EstadoAtencion.FINALIZADA;
	}

	/** RN50: solo la especialista asignada, o la Administradora, inicia y finaliza la atención (403). */
	public void verificarEspecialista(Long idPersonalSesion, boolean esAdministradora) {
		if (!esAdministradora && !idPersonal.equals(idPersonalSesion)) {
			throw new ReglaNegocioException("RN50", "Solo la especialista asignada o la Administradora atiende esta atención.",
					HttpStatus.FORBIDDEN);
		}
	}

	/** RF16: la especialista recomienda un producto una sola vez por atención (UQ_RECOM_ATENCION_PRODUCTO). */
	public void recomendarProducto(Long idProducto, String observacion) {
		boolean yaRecomendado = recomendaciones.stream().anyMatch(recomendacion -> recomendacion.getIdProducto().equals(idProducto));
		if (yaRecomendado) {
			throw new ReglaNegocioException("RF16", "El producto ya se recomendó en esta atención.", HttpStatus.CONFLICT);
		}
		recomendaciones.add(new RecomendacionProducto(this, idProducto, observacion));
	}

	public List<RecomendacionProducto> getRecomendaciones() {
		return Collections.unmodifiableList(recomendaciones);
	}

	/** RN36 y RN38: la atención cubierta por un paquete no se cobra ni comisiona. */
	public void usarSesion(Long idClientePaquete) {
		if (idCliente == null) {
			throw new ReglaNegocioException("RN36", "Una sesión de paquete solo la usa una clienta registrada.");
		}
		this.idClientePaquete = idClientePaquete;
	}

}
