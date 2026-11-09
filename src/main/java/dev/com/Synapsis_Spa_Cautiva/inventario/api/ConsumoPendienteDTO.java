package dev.com.Synapsis_Spa_Cautiva.inventario.api;

import java.time.LocalDateTime;

/**
 * [It. 2 · RN53] Lo que faltó descontar de un insumo al finalizar una atención (CONSUMO_PENDIENTE), con el código, el nombre y la unidad
 * del producto. Lo devuelve el fin de la atención (CUS11) y lo lista, regulariza o descarta almacén (CUS33).
 */
public record ConsumoPendienteDTO(Long idConsumo, Long idAtencion, Long idProducto, String codigo, String producto, String unidad, Long idSede,
		Integer cantidadEstimada, Integer cantidadDescontada, Integer cantidadPendiente, String estado, LocalDateTime fechaRegistro,
		Long idMovimiento, String observacion) {
}
