package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoAtencion;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.ConsumoPendienteDTO;

/**
 * [It. 2 · RN53] consumosPendientes solo viene al finalizar: los insumos cuyo consumo estimado no alcanzó el stock de la sede y quedaron
 * para que almacén los regularice (vacía si alcanzó todo). Al iniciar y al retomar la atención es null.
 */
public record AtencionDTO(Long idAtencion, Long idCita, Long idCliente, Long idPersonal, Long idRecurso, Long idVariante, Long idClientePaquete,
		LocalDateTime horaInicio, LocalDateTime horaFin, Integer tiempoDeclaradoMin, String observaciones, EstadoAtencion estado,
		List<ConsumoPendienteDTO> consumosPendientes) {

	public AtencionDTO conConsumosPendientes(List<ConsumoPendienteDTO> pendientes) {
		return new AtencionDTO(idAtencion, idCita, idCliente, idPersonal, idRecurso, idVariante, idClientePaquete, horaInicio, horaFin,
				tiempoDeclaradoMin, observaciones, estado, pendientes);
	}

}
