package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.service.ServicioAtencion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/atenciones")
@RequiredArgsConstructor
public class ControladorAtencion {

	private final ServicioAtencion servicioAtencion;

	/** [It. 2] CUS11: la agenda del día de la especialista de la sesión (RF13). Sin fecha, la de hoy. */
	@GetMapping("/mi-agenda")
	@PreAuthorize("hasAuthority('ATENCION:VER')")
	public ResponseEntity<List<CitaAtencionDTO>> listarMiAgenda(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return ResponseEntity.ok(servicioAtencion.listarMiAgenda(fecha));
	}

	/** [It. 2] CUS11: retoma la atención EN_CURSO de la cita desde otro equipo. */
	@GetMapping("/en-curso")
	@PreAuthorize("hasAuthority('ATENCION:VER')")
	public ResponseEntity<AtencionDTO> obtenerEnCurso(@RequestParam Long idCita) {
		return ResponseEntity.ok(servicioAtencion.obtenerEnCurso(idCita));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ATENCION:CREAR')")
	public ResponseEntity<AtencionDTO> iniciar(@Valid @RequestBody InicioAtencionDTO oInicioAtencionDTO) {
		AtencionDTO creada = servicioAtencion.iniciar(oInicioAtencionDTO.idCita(), oInicioAtencionDTO.tiempoDeclaradoMin());
		return ResponseEntity.created(URI.create("/api/v1/atenciones/" + creada.idAtencion())).body(creada);
	}

	@PatchMapping("/{idAtencion}/fin")
	@PreAuthorize("hasAuthority('ATENCION:EDITAR')")
	public ResponseEntity<AtencionDTO> finalizar(@PathVariable Long idAtencion, @Valid @RequestBody DetalleAtencionDTO oDetalleAtencionDTO) {
		return ResponseEntity.ok(servicioAtencion.finalizar(idAtencion, oDetalleAtencionDTO));
	}

}
