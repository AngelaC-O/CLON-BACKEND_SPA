package dev.com.Synapsis_Spa_Cautiva.pagos.repository;

import java.math.BigDecimal;

/** Proyección de V_PASARELA_LIQUIDACION: cobros, devoluciones, comisiones y abonos del mes por sede y medio (CUS47, RF105). */
public interface FilaLiquidacionMes {

	Long getIdSede();

	String getMedioPago();

	BigDecimal getCobros();

	BigDecimal getDevoluciones();

	BigDecimal getComisiones();

	BigDecimal getNetoEsperado();

	BigDecimal getNetoAbonado();

	Long getCobrosSinAbono();

	Long getSinComisionRegistrada();

}
