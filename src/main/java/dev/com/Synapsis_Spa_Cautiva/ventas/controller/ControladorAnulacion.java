package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.ServicioAnulacion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [It. 2] CUS22 a CUS24. La anulación ya no es una sola operación con la clave de la Administradora: la recepcionista la pide (VENTAS:ANULAR) y
 * la Administradora la resuelve en su bandeja y devuelve los pagos (VENTAS:EDITAR, que solo tiene ella). La sede sale de la sesión.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ControladorAnulacion {

	private final ServicioAnulacion servicioAnulacion;

	private final UsuarioActual usuarioActual;

	/** CUS22: la solicitud queda PENDIENTE y la venta no cambia (RN32). */
	@PostMapping("/ventas/{idVenta}/solicitudes-anulacion")
	@PreAuthorize("hasAuthority('VENTAS:ANULAR')")
	public ResponseEntity<SolicitudAnulacionDTO> solicitarAnulacion(@PathVariable Long idVenta,
			@Valid @RequestBody SolicitudAnulacionDTO oSolicitudAnulacionDTO) {
		SolicitudAnulacionDTO creada = servicioAnulacion.solicitarAnulacion(idVenta, oSolicitudAnulacionDTO);
		return ResponseEntity.created(URI.create("/api/v1/solicitudes-anulacion/" + creada.idSolicitud())).body(creada);
	}

	/** CUS23: la bandeja de la sede; sin estado, las PENDIENTES. */
	@GetMapping("/solicitudes-anulacion")
	@PreAuthorize("hasAuthority('VENTAS:EDITAR')")
	public ResponseEntity<List<SolicitudAnulacionDTO>> listarPendientes(@RequestParam(required = false) EstadoSolicitudAnulacion estado) {
		return ResponseEntity.ok(servicioAnulacion.listarPendientes(usuarioActual.idSede(), estado));
	}

	/** CUS23: nota de crédito, venta ANULADA y solicitud APROBADA, en una transacción. */
	@PostMapping("/solicitudes-anulacion/{idSolicitud}/aprobacion")
	@PreAuthorize("hasAuthority('VENTAS:EDITAR')")
	public ResponseEntity<SolicitudAnulacionDTO> aprobar(@PathVariable Long idSolicitud) {
		return ResponseEntity.ok(servicioAnulacion.aprobar(idSolicitud));
	}

	/** CUS23: con su motivo; la venta sigue CERRADA. */
	@PostMapping("/solicitudes-anulacion/{idSolicitud}/rechazo")
	@PreAuthorize("hasAuthority('VENTAS:EDITAR')")
	public ResponseEntity<SolicitudAnulacionDTO> rechazar(@PathVariable Long idSolicitud, @Valid @RequestBody RechazoDTO oRechazoDTO) {
		return ResponseEntity.ok(servicioAnulacion.rechazar(idSolicitud, oRechazoDTO.motivoRechazo()));
	}

	/** CUS24: devuelve por la pasarela los pagos con tarjeta de la venta ANULADA (RN44). */
	@PostMapping("/ventas/{idVenta}/devolucion")
	@PreAuthorize("hasAuthority('VENTAS:EDITAR')")
	public ResponseEntity<DevolucionDTO> devolverPago(@PathVariable Long idVenta, @RequestBody DevolucionDTO oDevolucionDTO) {
		return ResponseEntity.ok(servicioAnulacion.devolverPago(idVenta, oDevolucionDTO.formaDevolucion()));
	}

	/** CUS24: Yape o Plin por QR, devueltos desde la app con su número de operación (RN44). */
	@PostMapping("/ventas/{idVenta}/devolucion-manual")
	@PreAuthorize("hasAuthority('VENTAS:EDITAR')")
	public ResponseEntity<DevolucionDTO> registrarDevolucionManual(@PathVariable Long idVenta, @RequestBody DevolucionDTO oDevolucionDTO) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(servicioAnulacion.registrarDevolucionManual(idVenta, oDevolucionDTO.idTransaccion(), oDevolucionDTO.codigoOperacion()));
	}

}
