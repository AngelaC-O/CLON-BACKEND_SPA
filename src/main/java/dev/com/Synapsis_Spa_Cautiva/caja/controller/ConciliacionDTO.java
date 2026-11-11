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
		Long sinJustificar, List<Item> items) {

	/** Una diferencia (CONCILIACION_ITEM): de una venta, de SUNAT o de la pasarela (CK_CONC_ITEM_ORIGEN). */
	public record Item(Long idItem, Long idVenta, String referenciaSunat, Long idTransaccion, Boolean aceptadoSunat, Boolean pagoRespaldado,
			EstadoConciliacionItem estado, String justificacion) {
	}

}
