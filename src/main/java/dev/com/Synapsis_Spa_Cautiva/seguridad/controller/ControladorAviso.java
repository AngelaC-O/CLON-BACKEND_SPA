package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoDTO;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.ServicioAviso;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** [It. 2] Panel de avisos de Inicio (CUS50, RF47, RF101): cualquier usuario con sesión; la sede y los permisos salen de la sesión. */
@RestController
@RequestMapping("/api/v1/avisos")
@RequiredArgsConstructor
public class ControladorAviso {

	private final ServicioAviso servicioAviso;

	private final UsuarioActual usuarioActual;

	@GetMapping
	public ResponseEntity<List<AvisoDTO>> listarAvisos(Authentication autenticacion) {
		List<String> permisos = autenticacion.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
		return ResponseEntity.ok(servicioAviso.listarAvisos(usuarioActual.idSede(), permisos));
	}

}
