package dev.com.Synapsis_Spa_Cautiva.reportes.repository;

import java.math.BigDecimal;

/**
 * Una fila del reporte de ventas (CUS48, RF68): el grupo (día, mes, línea, servicio, especialista, tipo de atención o medio de
 * pago), cuántas ventas o unidades y el importe. Las ventas ANULADAS no se cuentan (RN33).
 */
public record FilaReporteDTO(String grupo, Long cantidad, BigDecimal importe) {
}
