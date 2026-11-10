package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.ServicioSerieComprobante;
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

/** [It. 2] CUS63: las series de comprobante de una sede (sin idSede, la de la sesión). */
@RestController
@RequestMapping("/api/v1/series-comprobante")
@RequiredArgsConstructor
public class ControladorSerieComprobante {

	private final ServicioSerieComprobante servicioSerieComprobante;

	private final UsuarioActual usuarioActual;

	@GetMapping
	@PreAuthorize("hasAuthority('CATALOGO:VER')")
	public ResponseEntity<List<SerieDTO>> listarSeries(@RequestParam(required = false) Long idSede) {
		return ResponseEntity.ok(servicioSerieComprobante.listarSeries(idSede == null ? usuarioActual.idSede() : idSede));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('CATALOGO:CREAR')")
	public ResponseEntity<SerieDTO> registrarSerie(@Valid @RequestBody SerieDTO oSerieDTO) {
		SerieDTO creada = servicioSerieComprobante.registrarSerie(oSerieDTO);
		return ResponseEntity.created(URI.create("/api/v1/series-comprobante/" + creada.idSerie())).body(creada);
	}

}
