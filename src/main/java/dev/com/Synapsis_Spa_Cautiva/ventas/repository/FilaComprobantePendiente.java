package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.time.LocalDateTime;

/** Proyección de V_CPE_PENDIENTES, que no tiene entidad. */
public interface FilaComprobantePendiente {

	Long getIdDocumento();

	String getTipo();

	String getSerie();

	String getNumero();

	String getEstadoSunat();

	LocalDateTime getFechaLimite();

}
