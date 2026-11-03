package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;

/** Los datos de la representación impresa (RN07). Aún no es un PDF: ver D-11 en docs/NOTAS_IT1.md. */
public record PdfConQr(Long idComprobante, TipoComprobante tipo, String serie, String numero, LocalDate fechaEmision, String nroDocComprador,
		String nombreComprador, BigDecimal total, EstadoSunat estadoSunat, String hashCpe, String textoQr, String contenidoXml) {
}
