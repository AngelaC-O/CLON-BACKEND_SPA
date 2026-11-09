package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.util.List;
import java.util.Set;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.ServicioAutenticacion;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.UsuarioSesion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class ControladorAutenticacion {

	private static final int SEGUNDOS_POR_MINUTO = 60;

	private final ServicioAutenticacion servicioAutenticacion;

	private final UsuarioActual usuarioActual;

	private final ParametroApi parametroApi;

	private final SecurityContextRepository repositorioDeContexto;

	/** [It. 2] La sesión vigente al recargar, sin guardarla en el navegador (CUS50 modificado). Sin sesión, 401. */
	@GetMapping("/sesion")
	public ResponseEntity<SesionDTO> obtenerSesion() {
		return ResponseEntity.ok(servicioAutenticacion.obtenerSesion(usuarioActual.idUsuario()));
	}

	@PostMapping("/login")
	public ResponseEntity<SesionDTO> iniciarSesion(@Valid @RequestBody CredencialesDTO credenciales, HttpServletRequest request,
			HttpServletResponse response) {
		SesionDTO oSesionDTO = servicioAutenticacion.iniciarSesion(credenciales.nombreUsuario(), credenciales.clave());
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		int minutos = parametroApi.valorNumero("SESION_INACTIVIDAD_MIN").intValue();
		request.getSession(true).setMaxInactiveInterval(minutos * SEGUNDOS_POR_MINUTO);
		guardarEnSesion(new UsuarioSesion(oSesionDTO.idUsuario(), oSesionDTO.nombreUsuario(), oSesionDTO.idSede(), oSesionDTO.debeCambiarClave(),
				Set.copyOf(oSesionDTO.permisos())), request, response);
		return ResponseEntity.ok(oSesionDTO);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> cerrarSesion(HttpServletRequest request, HttpServletResponse response, Authentication autenticacion) {
		servicioAutenticacion.cerrarSesion(usuarioActual.idUsuario());
		new SecurityContextLogoutHandler().logout(request, response, autenticacion);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/cambio-clave")
	public ResponseEntity<Void> cambiarClave(@Valid @RequestBody CambioClaveDTO oCambioClaveDTO, HttpServletRequest request,
			HttpServletResponse response, Authentication autenticacion) {
		servicioAutenticacion.cambiarClave(usuarioActual.idUsuario(), oCambioClaveDTO.claveActual(), oCambioClaveDTO.claveNueva());
		UsuarioSesion sesion = (UsuarioSesion) autenticacion.getPrincipal();
		guardarEnSesion(sesion.conClaveVigente(), request, response);
		return ResponseEntity.ok().build();
	}

	private void guardarEnSesion(UsuarioSesion usuario, HttpServletRequest request, HttpServletResponse response) {
		List<SimpleGrantedAuthority> permisos = usuario.permisos().stream().map(SimpleGrantedAuthority::new).toList();
		SecurityContext contexto = SecurityContextHolder.createEmptyContext();
		contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null, permisos));
		SecurityContextHolder.setContext(contexto);
		repositorioDeContexto.saveContext(contexto, request, response);
	}

}
