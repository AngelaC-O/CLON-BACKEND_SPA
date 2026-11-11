package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.util.List;

/**
 * [It. 2] SPI: los productos que compró una clienta, para su ficha (CUS03, RF02). Lo implementa ventas y lo usa agenda, que no puede depender
 * de ventas porque ventas ya depende de agenda (AtencionApi, SaldoFavorApi, PaqueteClienteApi) y Spring Modulith rechazaría el ciclo.
 */
public interface HistorialComprasProvider {

	/** Las líneas de producto de las ventas CERRADAS de la clienta, de la más reciente a la más antigua (vacía si no compró nada). */
	List<ProductoCompradoDTO> productosComprados(Long idCliente);

}
