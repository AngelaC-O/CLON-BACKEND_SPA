package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import dev.com.Synapsis_Spa_Cautiva.ventas.service.PdfConQr;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.ServicioFacturacionElectronica;
import lombok.RequiredArgsConstructor;

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
	public ResponseEntity<PdfConQr> obtenerRepresentacion(@PathVariable Long idComprobante) {
		return ResponseEntity.ok(servicioFacturacionElectronica.generarRepresentacionImpresa(idComprobante));
	}

	@GetMapping("/{idComprobante}/enlace")
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<EnlaceComprobanteDTO> obtenerEnlace(@PathVariable Long idComprobante, @RequestParam(required = false) String canal,
			@RequestParam(required = false) String correo) {
		return ResponseEntity.ok(new EnlaceComprobanteDTO(servicioFacturacionElectronica.generarEnlacePublico(idComprobante), canal, correo));
	}

}
