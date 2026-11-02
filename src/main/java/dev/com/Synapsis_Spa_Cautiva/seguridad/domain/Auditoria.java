package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "AUDITORIA")
@Getter
public class Auditoria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_auditoria")
	private Long idAuditoria;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime fechaHora;

	@Column(name = "modulo", nullable = false, length = 40)
	private String modulo;

	@Column(name = "accion", nullable = false, length = 40)
	private String accion;

	@Column(name = "entidad", nullable = false, length = 40)
	private String entidad;

	@Column(name = "id_entidad")
	private Integer idEntidad;

	@Column(name = "detalle", length = 400)
	private String detalle;

	protected Auditoria() {
	}

	public Auditoria(Long idUsuario, LocalDateTime fechaHora, String modulo, String accion, String entidad, Integer idEntidad,
			String detalle) {
		this.idUsuario = idUsuario;
		this.fechaHora = fechaHora;
		this.modulo = modulo;
		this.accion = accion;
		this.entidad = entidad;
		this.idEntidad = idEntidad;
		this.detalle = detalle;
	}

}
