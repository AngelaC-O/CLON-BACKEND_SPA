package dev.com.Synapsis_Spa_Cautiva.pagos.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [It. 2 · CUS47, RF105] Una fila del reporte de liquidación de Izipay: el código de operación del cobro, su comisión y la fecha en que la
 * pasarela lo abonó. La lee LectorReporteLiquidacion (caja) y la registra PagosApi.registrarLiquidacion. El diagrama solo nombra el tipo.
 */
public record FilaLiquidacionDTO(String codigoOperacion, BigDecimal comision, LocalDate fechaAbono) {
}
