package dev.com.Synapsis_Spa_Cautiva.sedes.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.sedes.service.ServicioRecurso;
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

@RestController
@RequestMapping("/api/v1/recursos")
@RequiredArgsConstructor
public class ControladorRecurso {

	private final ServicioRecurso servicioRecurso;

	private final MapperRecurso mapperRecurso;

	/** [It. 2] CUS61: los recursos de la sede con su tipo, estado y limpieza (diferencia 18 del front-end). */
	@GetMapping
	@PreAuthorize("hasAuthority('SEDES:VER')")
	public ResponseEntity<List<RecursoDTO>> listarRecursos(@RequestParam Long idSede) {
		return ResponseEntity.ok(mapperRecurso.aDTOs(servicioRecurso.listarRecursos(idSede)));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('SEDES:CREAR')")
	public ResponseEntity<RecursoDTO> registrarRecurso(@Valid @RequestBody RecursoDTO oRecursoDTO) {
		RecursoDTO creado = servicioRecurso.registrarRecurso(oRecursoDTO);
		return ResponseEntity.created(URI.create("/api/v1/recursos/" + creado.idRecurso())).body(creado);
	}

}
