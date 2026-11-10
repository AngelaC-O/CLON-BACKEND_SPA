package dev.com.Synapsis_Spa_Cautiva.pagos.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * [It. 2 · CUS47] Una transacción APROBADA del mes con su comisión y su abono, con la venta que respalda (VENTA_PAGO). fechaAbono es un DATE de
 * Oracle, que el driver entrega como LocalDateTime.
 */
public interface FilaTransaccionLiquidada {

	Long getIdTransaccion();

	Long getIdVenta();

	Long getIdCita();

	String getTipoOperacion();

	String getMedioPago();

	BigDecimal getMonto();

	BigDecimal getComision();

	LocalDateTime getFechaAbono();

}
