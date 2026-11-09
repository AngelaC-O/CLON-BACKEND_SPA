package dev.com.Synapsis_Spa_Cautiva.inventario.api;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.api.InsumoEstimadoDTO;

/** [It. 2] Lo que inventario publica: ventas (CUS25, CUS26) y agenda (CUS11, CUS12) lo usan por aquí. */
public interface InventarioApi {

	/** RF35: productos de VENTA activos con su precio y su stock en la sede; los insumos de uso interno no aparecen. Filtros opcionales. */
	List<ProductoStockDTO> buscar(String nombre, String marca, Long idCategoria, Long idSede);

	/** RF37, RN15: true si la sede tiene la cantidad; si no alcanza responde 409 con RN15. */
	boolean verificarStock(Long idProducto, Long idSede, Integer cantidad);

	/**
	 * RF46, RN53, RN54: descuenta el consumo estimado de la atención con una SALIDA por insumo ligada a ella; lo que no alcanza queda en
	 * CONSUMO_PENDIENTE. Devuelve los pendientes creados (vacía si alcanzó todo). Corre en la transacción del fin de la atención.
	 */
	List<ConsumoPendienteDTO> registrarConsumoAtencion(Long idAtencion, Long idSede, List<InsumoEstimadoDTO> insumos);

}
