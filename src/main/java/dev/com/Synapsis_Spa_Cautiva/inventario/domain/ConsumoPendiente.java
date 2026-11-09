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

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.Getter;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Raíz de su propio agregado (RN53): lo que faltó descontar del consumo estimado de un insumo al finalizar una atención.
 * La atención, el producto y la sede van por id. TRG_CONSUMO_PENDIENTE_REGLAS fija fecha_registro al insertar y quién y
 * cuándo lo resolvió; exige además la SALIDA de la regularización (-20105) y que lo resuelva almacén o la Administradora
 * (-20104). Hibernate vuelve a leer esas columnas al guardar.
 */
@Entity
@Table(name = "CONSUMO_PENDIENTE")
@DynamicUpdate
@Getter
public class ConsumoPendiente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_consumo")
	private Long idConsumo;

	@Column(name = "id_atencion", nullable = false, updatable = false)
	private Long idAtencion;

	@Column(name = "id_producto", nullable = false, updatable = false)
	private Long idProducto;

	@Column(name = "id_sede", nullable = false, updatable = false)
	private Long idSede;

	@Column(name = "cantidad_estimada", nullable = false, updatable = false)
	private Integer cantidadEstimada;

	@Column(name = "cantidad_descontada", nullable = false, updatable = false)
	private Integer cantidadDescontada;

	@Column(name = "cantidad_pendiente", nullable = false, updatable = false)
	private Integer cantidadPendiente;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 12)
	private EstadoConsumoPendiente estado;

	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "fecha_registro", nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "id_usuario_resuelve")
	private Long idUsuarioResuelve;

	@Generated(event = EventType.UPDATE, writable = true)
	@Column(name = "fecha_resolucion")
	private LocalDateTime fechaResolucion;

	@Column(name = "id_movimiento")
	private Long idMovimiento;

	@Column(name = "observacion", length = 300)
	private String observacion;

	protected ConsumoPendiente() {
	}

	/**
	 * RN53, RN54: lo estimado (SERVICIO_INSUMO.cantidad_estimada redondeada hacia arriba, en la unidad mínima) y lo que había
	 * en stock al finalizar la atención. cantidad_pendiente es la diferencia (CK_CONSUMO_PEND_CANTIDADES).
	 */
	public ConsumoPendiente(Long idAtencion, Long idProducto, Long idSede, Integer cantidadEstimada, Integer cantidadDescontada) {
		if (cantidadEstimada == null || cantidadDescontada == null || cantidadDescontada < 0 || cantidadDescontada >= cantidadEstimada) {
			throw new ReglaNegocioException("RN53", "Solo queda pendiente lo que faltó descontar del consumo estimado.");
		}
		this.idAtencion = idAtencion;
		this.idProducto = idProducto;
		this.idSede = idSede;
		this.cantidadEstimada = cantidadEstimada;
		this.cantidadDescontada = cantidadDescontada;
		this.cantidadPendiente = cantidadEstimada - cantidadDescontada;
		this.estado = EstadoConsumoPendiente.PENDIENTE;
		this.fechaRegistro = LocalDateTime.now();
	}

	/** RN53: la encargada de almacén lo regulariza con una SALIDA del mismo insumo, sede y atención (CUS33). */
	public void regularizar(Long idUsuarioResuelve, Long idMovimiento) {
		exigirPendiente();
		if (idMovimiento == null) {
			throw new ReglaNegocioException("RN53", "La regularización se hace con una salida de kardex.");
		}
		this.idMovimiento = idMovimiento;
		resolver(EstadoConsumoPendiente.REGULARIZADO, idUsuarioResuelve);
	}

	/** RN53: o lo descarta con su motivo, por ejemplo porque el consumo real fue menor (CUS33). */
	public void descartar(Long idUsuarioResuelve, String observacion) {
		exigirPendiente();
		if (observacion == null || observacion.isBlank()) {
			throw new ReglaNegocioException("RN53", "Indique el motivo del descarte.");
		}
		this.observacion = observacion.trim();
		resolver(EstadoConsumoPendiente.DESCARTADO, idUsuarioResuelve);
	}

	private void resolver(EstadoConsumoPendiente estado, Long idUsuarioResuelve) {
		this.estado = estado;
		this.idUsuarioResuelve = idUsuarioResuelve;
		this.fechaResolucion = LocalDateTime.now();
	}

	private void exigirPendiente() {
		if (estado != EstadoConsumoPendiente.PENDIENTE) {
			throw new ReglaNegocioException("RN53", "El consumo pendiente ya está " + estado + " y no cambia.");
		}
	}

}
