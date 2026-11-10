package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.seguridad.service.ServicioParametro;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** [It. 2] CUS65: parámetros del negocio y tipos de documento. Los tipos los lee cualquier usuario con sesión (los usan varias pantallas). */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ControladorParametro {

	private final ServicioParametro servicioParametro;

	@GetMapping("/parametros")
	@PreAuthorize("hasAuthority('SEGURIDAD:VER')")
	public ResponseEntity<List<ParametroDTO>> listarParametros() {
		return ResponseEntity.ok(servicioParametro.listarParametros());
	}

	@PatchMapping("/parametros/{codigo}")
	@PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
	public ResponseEntity<ParametroDTO> cambiarValor(@PathVariable String codigo, @Valid @RequestBody ParametroDTO oParametroDTO) {
		return ResponseEntity.ok(servicioParametro.cambiarValor(codigo, oParametroDTO));
	}

	@GetMapping("/tipos-documento")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<TipoDocumentoDTO>> listarTiposDocumento() {
		return ResponseEntity.ok(servicioParametro.listarTiposDocumento());
	}

	@PatchMapping("/tipos-documento/{idTipoDocumento}")
	@PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
	public ResponseEntity<TipoDocumentoDTO> configurarTipoDocumento(@PathVariable Long idTipoDocumento, @RequestBody TipoDocumentoDTO oTipoDocumentoDTO) {
		return ResponseEntity.ok(servicioParametro.configurarTipoDocumento(idTipoDocumento, oTipoDocumentoDTO));
	}

}
