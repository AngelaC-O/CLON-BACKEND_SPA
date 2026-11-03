package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.seguridad.service.ServicioUsuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ControladorUsuario {

	private final ServicioUsuario servicioUsuario;

	@GetMapping("/roles")
	@PreAuthorize("hasAuthority('SEGURIDAD:VER')")
	public ResponseEntity<List<RolDTO>> listarRoles() {
		return ResponseEntity.ok(servicioUsuario.listarRoles());
	}

	@PostMapping("/usuarios")
	@PreAuthorize("hasAuthority('SEGURIDAD:CREAR')")
	public ResponseEntity<UsuarioDTO> registrar(@Valid @RequestBody UsuarioDTO oUsuarioDTO) {
		UsuarioDTO creado = servicioUsuario.registrar(oUsuarioDTO);
		return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.idUsuario())).body(creado);
	}

	@PatchMapping("/usuarios/{idUsuario}/rol")
	@PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
	public ResponseEntity<UsuarioDTO> asignarRol(@PathVariable Long idUsuario, @Valid @RequestBody AsignarRolDTO oAsignarRolDTO) {
		return ResponseEntity.ok(servicioUsuario.asignarRol(idUsuario, oAsignarRolDTO.idRol()));
	}

	@PutMapping("/roles/{idRol}/permisos")
	@PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
	public ResponseEntity<RolDTO> configurarPermisos(@PathVariable Long idRol, @Valid @RequestBody RolDTO oRolDTO) {
		return ResponseEntity.ok(servicioUsuario.configurarPermisos(idRol, oRolDTO));
	}

}
