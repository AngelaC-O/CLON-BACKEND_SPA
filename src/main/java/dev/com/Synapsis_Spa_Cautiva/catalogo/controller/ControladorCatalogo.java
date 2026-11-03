package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.service.ServicioCatalogo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ControladorCatalogo {

	private final ServicioCatalogo servicioCatalogo;

	@GetMapping("/areas")
	@PreAuthorize("hasAuthority('CATALOGO:VER')")
	public ResponseEntity<List<AreaDTO>> listarAreas() {
		return ResponseEntity.ok(servicioCatalogo.listarAreas());
	}

	@PostMapping("/areas")
	@PreAuthorize("hasAuthority('CATALOGO:CREAR')")
	public ResponseEntity<AreaDTO> registrarArea(@Valid @RequestBody AreaDTO oAreaDTO) {
		AreaDTO creada = servicioCatalogo.registrarArea(oAreaDTO.linea(), oAreaDTO.nombreArea());
		return ResponseEntity.created(URI.create("/api/v1/areas/" + creada.idArea())).body(creada);
	}

	@PostMapping("/servicios")
	@PreAuthorize("hasAuthority('CATALOGO:CREAR')")
	public ResponseEntity<ServicioDTO> registrarServicio(@Valid @RequestBody ServicioDTO oServicioDTO) {
		ServicioDTO creado = servicioCatalogo.registrarServicio(oServicioDTO);
		return ResponseEntity.created(URI.create("/api/v1/servicios/" + creado.idServicio())).body(creado);
	}

}
