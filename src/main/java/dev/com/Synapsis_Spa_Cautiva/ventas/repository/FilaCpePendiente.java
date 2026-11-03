package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.time.LocalDateTime;

/** Proyección de V_CPE_PENDIENTES para el reenvío. */
public interface FilaCpePendiente {

	Long getIdDocumento();

	String getSerie();

	String getNumero();

	LocalDateTime getFechaLimite();

}
