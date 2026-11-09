package dev.com.Synapsis_Spa_Cautiva.catalogo.api;

/**
 * [It. 2 · RN53, RN54] Un insumo de la variante (SERVICIO_INSUMO) y lo que consume una atención, en su unidad mínima y redondeado hacia
 * arriba a entero. Lo pide agenda al finalizar la atención (CUS11) y lo recibe InventarioApi.registrarConsumoAtencion.
 */
public record InsumoEstimadoDTO(Long idProducto, Integer cantidadEstimada) {
}
