package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoConciliacion;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoConciliacionItem;

/**
 * [It. 2 · CUS47, RF75, RF76, RN31] La conciliación del mes con sus diferencias. En el POST solo van anio y mes. sinJustificar: cuántas
 * diferencias faltan para poder cerrarla. El diagrama solo nombra el tipo.
 */
public record ConciliacionDTO(Long idConciliacion, Integer anio, Integer mes, Long idUsuario, EstadoConciliacion estado, LocalDateTime fechaCierre,
		Long sinJustificar, List<ItemConciliacion> items) {

	/**
	 * Una diferencia (CONCILIACION_ITEM): de una venta, de SUNAT o de la pasarela (CK_CONC_ITEM_ORIGEN). [It. 2.1] Antes se llamaba Item, igual
	 * que ProformaDTO.Item: springdoc usa el nombre simple y el contrato apuntaba al ítem de la proforma (Duda 22 del front-end).
	 */
	public record ItemConciliacion(Long idItem, Long idVenta, String referenciaSunat, Long idTransaccion, Boolean aceptadoSunat, Boolean pagoRespaldado,
			EstadoConciliacionItem estado, String justificacion) {
	}

}
