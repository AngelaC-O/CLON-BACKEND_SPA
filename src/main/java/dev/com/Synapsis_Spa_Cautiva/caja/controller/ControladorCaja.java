package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.caja.service.ServicioCaja;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** La sede sale de la sesión. */
@RestController
@RequestMapping("/api/v1/cajas")
@RequiredArgsConstructor
public class ControladorCaja {

	private final ServicioCaja servicioCaja;

	private final UsuarioActual usuarioActual;

	/** [It. 2] CUS44: 200 con la caja ABIERTA de la sede de la sesión o 204 si no tiene. */
	@GetMapping("/abierta")
	@PreAuthorize("hasAuthority('CAJA:VER')")
	public ResponseEntity<CajaDTO> obtenerAbierta() {
		return servicioCaja.obtenerAbierta(usuarioActual.idSede()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping
	@PreAuthorize("hasAuthority('CAJA:CREAR')")
	public ResponseEntity<CajaDTO> abrir(@Valid @RequestBody AperturaCajaDTO oAperturaCajaDTO) {
		CajaDTO creada = servicioCaja.abrir(usuarioActual.idSede(), oAperturaCajaDTO.montoInicial());
		return ResponseEntity.created(URI.create("/api/v1/cajas/" + creada.idCaja())).body(creada);
	}

	@GetMapping("/{idCaja}/esperado")
	@PreAuthorize("hasAuthority('CAJA:VER')")
	public ResponseEntity<EsperadoDTO> calcularEsperado(@PathVariable Long idCaja) {
		return ResponseEntity.ok(servicioCaja.calcularEsperado(idCaja));
	}

	/**
	 * [It. 2] CUS46: cierres con diferencia por aprobar (RF67). Aprobar y reabrir usan CONCILIACION y no CAJA: la recepcionista tiene CAJA:EDITAR
	 * para cerrar su caja y no debe aprobarla ni reabrirla.
	 */
	@GetMapping("/con-diferencia")
	@PreAuthorize("hasAuthority('CONCILIACION:VER')")
	public ResponseEntity<List<CajaDTO>> listarConDiferencia() {
		return ResponseEntity.ok(servicioCaja.listarConDiferencia(usuarioActual.idSede()));
	}

	/** [It. 2] CUS46: la caja sigue CERRADA y queda revisada (RF67). */
	@PatchMapping("/{idCaja}/revision")
	@PreAuthorize("hasAuthority('CONCILIACION:EDITAR')")
	public ResponseEntity<CajaDTO> aprobarCierre(@PathVariable Long idCaja) {
		return ResponseEntity.ok(servicioCaja.aprobarCierre(idCaja));
	}

	/** [It. 2] CUS46: las cajas de hoy de la sede de la sesión (RF66). */
	@GetMapping("/del-dia")
	@PreAuthorize("hasAuthority('CONCILIACION:VER')")
	public ResponseEntity<List<CajaDTO>> obtenerDelDia() {
		return ResponseEntity.ok(servicioCaja.obtenerDelDia(usuarioActual.idSede()));
	}

	/** [It. 2] CUS46: reabre la caja CERRADA del día con un motivo (RF66, RN30). */
	@PatchMapping("/{idCaja}/reapertura")
	@PreAuthorize("hasAuthority('CONCILIACION:EDITAR')")
	public ResponseEntity<CajaDTO> reabrir(@PathVariable Long idCaja, @Valid @RequestBody ReaperturaDTO oReaperturaDTO) {
		return ResponseEntity.ok(servicioCaja.reabrir(idCaja, oReaperturaDTO.motivoReapertura()));
	}

	@PostMapping("/{idCaja}/cierre")
	@PreAuthorize("hasAuthority('CAJA:EDITAR')")
	public ResponseEntity<CajaDTO> cerrar(@PathVariable Long idCaja, @Valid @RequestBody ArqueoDTO oArqueoDTO) {
		return ResponseEntity.ok(servicioCaja.cerrar(idCaja, oArqueoDTO));
	}

}
