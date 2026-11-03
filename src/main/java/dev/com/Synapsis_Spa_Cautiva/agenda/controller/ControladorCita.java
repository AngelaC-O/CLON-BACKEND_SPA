package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.service.ServicioCita;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.EnlacePagoDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
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

/** La sede sale de la sesión. */
@RestController
@RequestMapping("/api/v1/citas")
@RequiredArgsConstructor
public class ControladorCita {

	private final ServicioCita servicioCita;

	private final UsuarioActual usuarioActual;

	@GetMapping("/disponibilidad")
	@PreAuthorize("hasAuthority('AGENDA:VER')")
	public ResponseEntity<List<HoraLibre>> consultarDisponibilidad(@RequestParam Long idVariante,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return ResponseEntity.ok(servicioCita.consultarDisponibilidad(idVariante, usuarioActual.idSede(), fecha));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('AGENDA:CREAR')")
	public ResponseEntity<CitaDTO> agendar(@Valid @RequestBody CitaDTO oCitaDTO) {
		CitaDTO creada = servicioCita.agendar(oCitaDTO);
		return ResponseEntity.created(URI.create("/api/v1/citas/" + creada.idCita())).body(creada);
	}

	@PostMapping("/lista-espera")
	@PreAuthorize("hasAuthority('AGENDA:CREAR')")
	public ResponseEntity<ListaEsperaDTO> registrarEspera(@Valid @RequestBody ListaEsperaDTO oListaEsperaDTO) {
		return ResponseEntity.status(HttpStatus.CREATED).body(servicioCita.registrarEspera(oListaEsperaDTO));
	}

	@PostMapping("/{idCita}/adelanto")
	@PreAuthorize("hasAuthority('AGENDA:EDITAR')")
	public ResponseEntity<CitaDTO> registrarAdelantoEfectivo(@PathVariable Long idCita, @Valid @RequestBody AdelantoDTO oAdelantoDTO) {
		return ResponseEntity.ok(servicioCita.registrarAdelantoEfectivo(idCita, oAdelantoDTO.adelantoMonto(), oAdelantoDTO.idMedioPago()));
	}

	@PostMapping("/{idCita}/enlace-adelanto")
	@PreAuthorize("hasAuthority('AGENDA:EDITAR')")
	public ResponseEntity<EnlacePagoDTO> generarEnlaceAdelanto(@PathVariable Long idCita, @Valid @RequestBody AdelantoDTO oAdelantoDTO) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(servicioCita.generarEnlaceAdelanto(idCita, oAdelantoDTO.adelantoMonto(), oAdelantoDTO.idMedioPago()));
	}

	@GetMapping("/agenda")
	@PreAuthorize("hasAuthority('AGENDA:VER')")
	public ResponseEntity<List<BloqueAgendaDTO>> consultarAgenda(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) Long idPersonal, @RequestParam(required = false) Long idRecurso) {
		return ResponseEntity.ok(servicioCita.consultarAgenda(usuarioActual.idSede(), fecha, idPersonal, idRecurso));
	}

}
