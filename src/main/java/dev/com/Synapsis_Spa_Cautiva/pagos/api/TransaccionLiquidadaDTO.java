package dev.com.Synapsis_Spa_Cautiva.pagos.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Transacción APROBADA del mes con su comisión y su abono del reporte de liquidación (CUS47, RF105). idVenta sale de
 * VENTA_PAGO (TRANSACCION_PASARELA no tiene id_venta) e idCita del adelanto. tipoOperacion va como texto porque el enum
 * del dominio es interno. Los campos salen de TRANSACCION_PASARELA; el diagrama solo nombra el tipo (DF-71).
 */
public record TransaccionLiquidadaDTO(Long idTransaccion, Long idVenta, Long idCita, String tipoOperacion, String medioPago,
		BigDecimal monto, BigDecimal comision, LocalDate fechaAbono) {

	private static final String COBRO = "COBRO";

	public boolean esCobro() {
		return COBRO.equals(tipoOperacion);
	}

	public boolean abonada() {
		return fechaAbono != null;
	}

}
