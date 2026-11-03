package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

/**
 * Solo estructura en la Iteración 1: sus CUS son de otra iteración.
 */
@Entity
@Table(name = "REQUERIMIENTO_COMPRA")
@Getter
public class RequerimientoCompra {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_requerimiento")
	private Long idRequerimiento;

	@Column(name = "numero", nullable = false, length = 10)
	private String numero;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Column(name = "area_solicitante", nullable = false, length = 40)
	private String areaSolicitante;

	@Column(name = "linea_produccion", nullable = false, length = 40)
	private String lineaProduccion;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_adquisicion", nullable = false, length = 10)
	private TipoAdquisicion tipoAdquisicion;

	@Enumerated(EnumType.STRING)
	@Column(name = "prioridad", nullable = false, length = 10)
	private PrioridadRequerimiento prioridad;

	@Column(name = "id_usuario_solicita", nullable = false)
	private Long idUsuarioSolicita;

	@Column(name = "fecha_solicitud", nullable = false)
	private LocalDateTime fechaSolicitud;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoRequerimientoCompra estado;

	@Column(name = "id_usuario_aprueba")
	private Long idUsuarioAprueba;

	@Column(name = "fecha_resolucion")
	private LocalDateTime fechaResolucion;

	@Column(name = "motivo_rechazo", length = 200)
	private String motivoRechazo;

	protected RequerimientoCompra() {
	}

}
