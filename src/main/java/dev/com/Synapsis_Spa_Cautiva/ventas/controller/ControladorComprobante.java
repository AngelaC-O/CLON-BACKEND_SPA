package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import dev.com.Synapsis_Spa_Cautiva.ventas.service.PdfConQr;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.ServicioFacturacionElectronica;
import lombok.RequiredArgsConstructor;

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

@RestController
@RequestMapping("/api/v1/comprobantes")
@RequiredArgsConstructor
public class ControladorComprobante {

	private final ServicioFacturacionElectronica servicioFacturacionElectronica;

	@GetMapping("/{idComprobante}/representacion")
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<byte[]> obtenerRepresentacion(@PathVariable Long idComprobante) {
		return pdf(servicioFacturacionElectronica.generarRepresentacionImpresa(idComprobante));
	}

	@GetMapping("/{idComprobante}/enlace")
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<EnlaceComprobanteDTO> obtenerEnlace(@PathVariable Long idComprobante, @RequestParam(required = false) String canal,
			@RequestParam(required = false) String correo) {
		return ResponseEntity.ok(new EnlaceComprobanteDTO(servicioFacturacionElectronica.generarEnlacePublico(idComprobante), canal, correo));
	}

	/** Sin sesión ni permiso: lo que vale es la firma con vencimiento. */
	@GetMapping("/publico/{idComprobante}")
	public ResponseEntity<byte[]> obtenerPublico(@PathVariable Long idComprobante, @RequestParam long expira, @RequestParam String firma) {
		return pdf(servicioFacturacionElectronica.obtenerRepresentacionPublica(idComprobante, expira, firma));
	}

	private static ResponseEntity<byte[]> pdf(PdfConQr oPdfConQr) {
		return ResponseEntity.ok()
			.contentType(MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(oPdfConQr.nombreArchivo()).build().toString())
			.body(oPdfConQr.contenido());
	}

}
