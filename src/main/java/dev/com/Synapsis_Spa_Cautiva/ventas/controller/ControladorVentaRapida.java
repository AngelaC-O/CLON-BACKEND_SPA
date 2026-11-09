package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.ServicioVentaRapida;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** La sede sale de la sesión. */
@RestController
@RequestMapping("/api/v1/ventas/rapidas")
@RequiredArgsConstructor
public class ControladorVentaRapida {

	private final ServicioVentaRapida servicioVentaRapida;

	private final UsuarioActual usuarioActual;

	@GetMapping("/libres")
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<List<HoraLibre>> libresAhora(@RequestParam Long idVariante) {
		return ResponseEntity.ok(servicioVentaRapida.libresAhora(idVariante, usuarioActual.idSede()));
	}

	/** 201 con la venta CERRADA; [It. 2 · RN52] 202 con la venta ABIERTA si el cobro digital quedó PENDIENTE (se cierra con POST /ventas/{idVenta}/cierre). */
	@PostMapping
	@PreAuthorize("hasAuthority('VENTAS:CREAR')")
	public ResponseEntity<VentaDTO> registrarYCobrar(@Valid @RequestBody VentaRapidaDTO oVentaRapidaDTO) {
		VentaDTO creada = servicioVentaRapida.registrarYCobrar(oVentaRapidaDTO);
		URI ubicacion = URI.create("/api/v1/ventas/" + creada.idVenta());
		if (!creada.cobrosPendientes().isEmpty()) {
			return ResponseEntity.accepted().location(ubicacion).body(creada);
		}
		return ResponseEntity.created(ubicacion).body(creada);
	}

}
