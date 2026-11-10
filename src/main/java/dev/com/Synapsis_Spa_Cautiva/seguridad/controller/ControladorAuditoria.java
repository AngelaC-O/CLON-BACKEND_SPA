package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.seguridad.service.ServicioAuditoria;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** [It. 2] CUS55: consulta de la auditoría (RF80, RN29); todos los filtros son opcionales. */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class ControladorAuditoria {

	private final ServicioAuditoria servicioAuditoria;

	@GetMapping
	@PreAuthorize("hasAuthority('SEGURIDAD:VER')")
	public ResponseEntity<List<AuditoriaDTO>> consultar(@RequestParam(required = false) Long idUsuario, @RequestParam(required = false) String modulo,
			@RequestParam(required = false) String entidad,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
		return ResponseEntity.ok(servicioAuditoria.consultar(idUsuario, modulo, entidad, fechaInicio, fechaFin));
	}

}
