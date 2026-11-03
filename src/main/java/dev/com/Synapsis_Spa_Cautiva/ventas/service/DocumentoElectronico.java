package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lo que el comprobante le manda al proveedor. El detalle de líneas lo agrega el adaptador real en B5. */
public record DocumentoElectronico(String tipo, String serie, String numero, LocalDate fechaEmision, Long idTipoDocComprador, String nroDocComprador,
		String nombreComprador, BigDecimal total) {
}
