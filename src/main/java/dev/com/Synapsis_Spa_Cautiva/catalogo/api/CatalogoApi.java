package dev.com.Synapsis_Spa_Cautiva.catalogo.api;

import java.math.BigDecimal;
import java.util.List;

public interface CatalogoApi {

	DatosVariante obtenerVariante(Long idVariante);

	/** [It. 2] CUS08: el nombre del servicio de cada franja de la agenda. */
	List<DatosVariante> obtenerVariantes(List<Long> idsVariante);

	/** Precio unitario de la variante o del paquete, con el descuento de la promoción vigente si se indica (RF31, RN25). */
	BigDecimal precioVigente(Long idVariante, Long idPaquete, Long idPromocion);

}
