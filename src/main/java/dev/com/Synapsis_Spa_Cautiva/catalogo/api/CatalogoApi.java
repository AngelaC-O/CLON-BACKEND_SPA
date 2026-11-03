package dev.com.Synapsis_Spa_Cautiva.catalogo.api;

import java.math.BigDecimal;

public interface CatalogoApi {

	DatosVariante obtenerVariante(Long idVariante);

	/** Precio unitario de la variante o del paquete, con el descuento de la promoción vigente si se indica (RF31, RN25). */
	BigDecimal precioVigente(Long idVariante, Long idPaquete, Long idPromocion);

}
