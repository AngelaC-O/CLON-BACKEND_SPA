package dev.com.Synapsis_Spa_Cautiva.catalogo.api;

import java.math.BigDecimal;
import java.util.List;

public interface CatalogoApi {

	DatosVariante obtenerVariante(Long idVariante);

	/**
	 * [It. 2] CUS08: el nombre del servicio de cada franja de la agenda. [It. 2.1 · nombres] En lote: una consulta por lista de ids; las variantes
	 * que no existen no vienen.
	 */
	List<DatosVariante> obtenerVariantes(List<Long> idsVariante);

	/** [It. 2.1 · nombres] El nombre de cada paquete, en lote (ficha de la clienta y detalle de la venta). Los que no existen no vienen. */
	List<DatosPaquete> obtenerPaquetes(List<Long> idsPaquete);

	/**
	 * [It. 2 · CUS11, RN53, RN54] Los insumos de la variante (SERVICIO_INSUMO) con lo que consume una atención, en su unidad mínima y
	 * redondeado hacia arriba a entero. Vacía si la variante no tiene insumos.
	 */
	List<InsumoEstimadoDTO> insumosDeVariante(Long idVariante);

	/** Precio unitario de la variante o del paquete, con el descuento de la promoción vigente si se indica (RF31, RN25). */
	BigDecimal precioVigente(Long idVariante, Long idPaquete, Long idPromocion);

}
