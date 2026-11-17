package dev.com.Synapsis_Spa_Cautiva.inventario.api;

/** [It. 2.1 · nombres] El nombre de un producto (y su unidad) para las pantallas y documentos de otros módulos: recomendaciones, venta y CPE. */
public record DatosProducto(Long idProducto, String nombre, String unidad) {
}
