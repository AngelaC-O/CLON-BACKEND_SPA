package dev.com.Synapsis_Spa_Cautiva.pagos.api;

import java.util.List;

/**
 * [It. 2 · CUS47, RF105] Lo que dejó la importación del reporte de liquidación: cuántas filas se leyeron, cuántas casaron con un cobro APROBADO
 * de TRANSACCION_PASARELA por su codigo_operacion y los códigos que no casaron (para revisarlos a mano). El diagrama solo nombra el tipo.
 */
public record ResumenImportacionDTO(int filasLeidas, int filasRegistradas, List<String> codigosSinTransaccion) {
}
