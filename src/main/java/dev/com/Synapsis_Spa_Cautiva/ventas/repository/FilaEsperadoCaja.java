package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.math.BigDecimal;

/** Proyección de V_CAJA_ESPERADO, que no tiene entidad. */
public interface FilaEsperadoCaja {

	Long getIdMedioPago();

	String getMedioPago();

	BigDecimal getEsperado();

}
