package dev.com.Synapsis_Spa_Cautiva.reportes.controller;

import java.time.LocalDate;

import dev.com.Synapsis_Spa_Cautiva.reportes.service.FormatoExportacion;
import dev.com.Synapsis_Spa_Cautiva.reportes.service.ServicioReporte;
import dev.com.Synapsis_Spa_Cautiva.reportes.service.TipoReporte;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [It. 2] CUS48 y CUS49: tipoReporte = ventas, productos, margen, demanda, ocupacion o especialistas. idSede nulo = consolidado (RF93). Las fechas
 * son opcionales en la firma para que su falta responda 422 con la regla y no 400.
 */
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ControladorReporte {

	private final ServicioReporte servicioReporte;

	@GetMapping("/{tipoReporte}")
	@PreAuthorize("hasAuthority('REPORTES:VER')")
	public ResponseEntity<ReporteDTO> generar(@PathVariable String tipoReporte,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin, @RequestParam(required = false) String agrupacion,
			@RequestParam(required = false) Long idSede) {
		return ResponseEntity.ok(servicioReporte.generar(tipoReporte, fechaInicio, fechaFin, agrupacion, idSede));
	}

	/** RF74: el archivo reporte_<tipo>_<desde>_<hasta>.<xlsx|pdf> como adjunto. */
	@GetMapping("/{tipoReporte}/exportacion")
	@PreAuthorize("hasAuthority('REPORTES:EXPORTAR')")
	public ResponseEntity<byte[]> exportar(@PathVariable String tipoReporte,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin, @RequestParam(required = false) String agrupacion,
			@RequestParam(required = false) Long idSede, @RequestParam(required = false) String formato) {
		byte[] archivo = servicioReporte.exportar(tipoReporte, fechaInicio, fechaFin, agrupacion, idSede, formato);
		FormatoExportacion oFormato = FormatoExportacion.de(formato);
		String nombre = "reporte_" + TipoReporte.deRuta(tipoReporte).ruta() + "_" + fechaInicio + "_" + fechaFin + "." + oFormato.extension();
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(oFormato.tipoContenido()))
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
			.body(archivo);
	}

}
