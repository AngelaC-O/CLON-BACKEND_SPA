package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import java.time.LocalDateTime;

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

import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "PERSONA")
@DynamicUpdate
@Getter
public class Persona {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_persona")
	private Long idPersona;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_persona", nullable = false, length = 10)
	private TipoPersona tipoPersona;

	@Column(name = "nombres", length = 80)
	private String nombres;

	@Column(name = "apellidos", length = 80)
	private String apellidos;

	@Column(name = "razon_social", length = 120)
	private String razonSocial;

	@Column(name = "id_tipo_documento")
	private Long idTipoDocumento;

	@Column(name = "numero_documento", length = 15)
	private String numeroDocumento;

	@Column(name = "telefono", length = 15)
	private String telefono;

	@Column(name = "correo", length = 100)
	private String correo;

	@Enumerated(EnumType.STRING)
	@Column(name = "origen_datos", nullable = false, length = 10)
	private OrigenDatos origenDatos;

	@Column(name = "fecha_verificacion")
	private LocalDateTime fechaVerificacion;

	@Column(name = "fecha_registro", updatable = false, nullable = false)
	private LocalDateTime fechaRegistro;

	protected Persona() {
	}

	public Persona(TipoPersona tipoPersona, String nombres, String apellidos, Long idTipoDocumento, String numeroDocumento,
			String telefono, String correo) {
		this.tipoPersona = tipoPersona;
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.idTipoDocumento = idTipoDocumento;
		this.numeroDocumento = numeroDocumento;
		this.telefono = telefono;
		this.correo = correo;
		this.origenDatos = OrigenDatos.MANUAL;
		this.fechaRegistro = LocalDateTime.now();
	}

	/** Si el documento cambia, TRG_PERSONA_REGLAS devuelve la ficha a MANUAL. */
	public void actualizar(String nombres, String apellidos, Long idTipoDocumento, String numeroDocumento, String telefono, String correo) {
		this.nombres = nombres;
		this.apellidos = apellidos;
		this.idTipoDocumento = idTipoDocumento;
		this.numeroDocumento = numeroDocumento;
		this.telefono = telefono;
		this.correo = correo;
	}

	/** RN45: solo queda VERIFICADO un documento consultado en APISPERU; si no, la ficha sigue MANUAL. */
	public void marcarVerificado(LocalDateTime fechaVerificacion) {
		if (numeroDocumento == null || fechaVerificacion == null) {
			throw new ReglaNegocioException("RN45", "Solo se verifica una persona con documento y fecha de consulta.");
		}
		this.origenDatos = OrigenDatos.VERIFICADO;
		this.fechaVerificacion = fechaVerificacion;
	}

}
