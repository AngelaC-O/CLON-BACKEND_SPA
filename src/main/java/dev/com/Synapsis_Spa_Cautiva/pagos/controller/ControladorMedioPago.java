package dev.com.Synapsis_Spa_Cautiva.pagos.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.pagos.service.ServicioPago;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** [It. 2] CUS62: los medios de pago. Los lee quien cobra (VENTAS:VER) y los configura la Administradora (CATALOGO:EDITAR). */
@RestController
@RequestMapping("/api/v1/medios-pago")
@RequiredArgsConstructor
public class ControladorMedioPago {

	private final ServicioPago servicioPago;

	@GetMapping
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<List<MedioPagoDTO>> listarMediosPago() {
		return ResponseEntity.ok(servicioPago.listarMediosPago());
	}

	@PatchMapping("/{idMedioPago}")
	@PreAuthorize("hasAuthority('CATALOGO:EDITAR')")
	public ResponseEntity<MedioPagoDTO> configurarMedioPago(@PathVariable Long idMedioPago, @RequestBody MedioPagoDTO oMedioPagoDTO) {
		return ResponseEntity.ok(servicioPago.configurarMedioPago(idMedioPago, oMedioPagoDTO.estado(), oMedioPagoDTO.usaPasarela()));
	}

}
