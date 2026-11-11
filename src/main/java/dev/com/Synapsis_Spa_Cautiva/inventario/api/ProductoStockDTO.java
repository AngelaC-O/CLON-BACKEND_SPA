package dev.com.Synapsis_Spa_Cautiva.inventario.api;

import java.math.BigDecimal;

/** [It. 2 · RF35] Un producto de VENTA con su precio y su stock en la sede (CUS25, CUS12). No trae el costo (RN24). */
public record ProductoStockDTO(Long idProducto, String codigo, String nombre, String marca, Long idCategoria, String unidad,
		BigDecimal precioVenta, Integer stockActual) {
}
