package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.net.URI;

import dev.com.Synapsis_Spa_Cautiva.agenda.service.ServicioAtencion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/atenciones")
@RequiredArgsConstructor
public class ControladorAtencion {

	private final ServicioAtencion servicioAtencion;

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
