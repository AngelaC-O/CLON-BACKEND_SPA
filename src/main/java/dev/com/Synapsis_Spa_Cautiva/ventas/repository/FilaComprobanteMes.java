package dev.com.Synapsis_Spa_Cautiva.ventas.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Proyección de V_COMPROBANTES_MES: boletas y facturas del mes con su nota de crédito, si la tienen (CUS47, RF75, RN31). */
public interface FilaComprobanteMes {

	Long getIdSede();

	String getTipo();

	String getSerie();

	String getNumero();

	LocalDate getFechaEmision();

	Long getIdVenta();

	BigDecimal getImporte();

	String getEstadoVenta();

	String getEstadoSunat();

	String getNcSerie();

	String getNcNumero();

	String getNcEstadoSunat();

}
