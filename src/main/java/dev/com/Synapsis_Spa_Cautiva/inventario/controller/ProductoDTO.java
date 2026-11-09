package dev.com.Synapsis_Spa_Cautiva.inventario.controller;

import java.math.BigDecimal;

import dev.com.Synapsis_Spa_Cautiva.inventario.domain.EstadoProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.TipoUsoProducto;

/**
 * CUS32 (RF44, RF45, RF86, RF87, RN13, RN16, RN54). Al registrar: margen es opcional (la encargada ajusta el de PARAMETRO antes de guardar)
 * y precioVenta lo calcula el servidor. En la respuesta, el stock es el de la sede de la sesión y margen solo viene para la Administradora
 * (RN24): la encargada ve y registra el costo, no el margen.
 */
public record ProductoDTO(Long idProducto, String codigo, String nombre, String marca, Long idCategoria, String unidad, TipoUsoProducto tipoUso,
		BigDecimal costo, BigDecimal margen, BigDecimal precioVenta, Boolean esPremium, EstadoProducto estado, Integer stockActual,
		Integer stockMinimo, String ubicacion) {
}
