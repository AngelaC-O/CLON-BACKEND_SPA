package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.EstadoTipoDocumento;

/**
 * [It. 2 · CUS65, RN45] Un tipo de documento (catálogo 06 de SUNAT) con su longitud y su formato, que carga el script. En el PATCH solo van
 * consultaApi y estado. Reemplaza la copia fija de los id 1 a 5 del front-end (Duda 5).
 */
public record TipoDocumentoDTO(Long idTipoDocumento, String codigoSunat, String nombre, String abreviatura, Integer longitudMin, Integer longitudMax,
		Boolean soloNumeros, Boolean consultaApi, EstadoTipoDocumento estado) {
}
