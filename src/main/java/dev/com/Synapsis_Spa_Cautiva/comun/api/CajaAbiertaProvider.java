package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.util.Optional;

/** SPI: la caja abierta de una sede. Lo implementa caja (ServicioCaja). */
public interface CajaAbiertaProvider {

	/** 422 si la sede no tiene caja abierta (RN30). */
	Long obtenerCajaAbierta(Long idSede);

	/** Para el adelanto digital, que no exige caja: la pasarela ya cobró. */
	Optional<Long> buscarCajaAbierta(Long idSede);

}
