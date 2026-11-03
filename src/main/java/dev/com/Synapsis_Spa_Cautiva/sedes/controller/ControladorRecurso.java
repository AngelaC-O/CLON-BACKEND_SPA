package dev.com.Synapsis_Spa_Cautiva.sedes.controller;

import java.net.URI;

import dev.com.Synapsis_Spa_Cautiva.sedes.service.ServicioRecurso;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recursos")
@RequiredArgsConstructor
public class ControladorRecurso {

	private final ServicioRecurso servicioRecurso;

	@PostMapping
	@PreAuthorize("hasAuthority('SEDES:CREAR')")
	public ResponseEntity<RecursoDTO> registrarRecurso(@Valid @RequestBody RecursoDTO oRecursoDTO) {
		RecursoDTO creado = servicioRecurso.registrarRecurso(oRecursoDTO);
		return ResponseEntity.created(URI.create("/api/v1/recursos/" + creado.idRecurso())).body(creado);
	}

}
