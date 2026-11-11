package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.personal.api.EspecialistaHorario;
import dev.com.Synapsis_Spa_Cautiva.personal.api.PersonalApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.MapperUsuario;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.PermisoDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.PersonalSinUsuarioDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.RolDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.UsuarioDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Rol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.RolPermiso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Usuario;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioPermiso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioRol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioUsuario;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioUsuario {

	private static final String SEGURIDAD = "SEGURIDAD";

	/** RN48: lo que el rol ADMINISTRADORA conserva siempre. */
	private static final List<String> ACCIONES_MINIMAS = List.of("VER", "CREAR", "EDITAR");

	private final RepositorioUsuario repositorioUsuario;

	private final CodificadorClave codificadorClave;

	private final RepositorioRol repositorioRol;

	private final RepositorioPermiso repositorioPermiso;

	private final PersonalApi personalApi;

	private final PersonaApi personaApi;

	private final MapperUsuario mapperUsuario;

	@Transactional(readOnly = true)
	public List<RolDTO> listarRoles() {
		return repositorioUsuario.findRolesActivos().stream().map(this::aDTO).toList();
	}

	/** [It. 2] CUS52 (D-06): el personal ACTIVO sin cuenta. PersonalApi da el id y el cargo; el nombre lo pone PersonaApi (DF-23). */
	@Transactional(readOnly = true)
	public List<PersonalSinUsuarioDTO> listarPersonalSinUsuario() {
		Set<Long> conUsuario = new HashSet<>(repositorioUsuario.findIdsPersonalConUsuario());
		List<EspecialistaHorario> sinUsuario = personalApi.listarPersonal().stream()
			.filter(oPersonal -> !conUsuario.contains(oPersonal.idPersonal()))
			.toList();
		Map<Long, PersonaDTO> personas = personaApi.obtenerPersonas(sinUsuario.stream().map(EspecialistaHorario::idPersonal).toList()).stream()
			.collect(Collectors.toMap(PersonaDTO::idPersona, Function.identity()));
		return sinUsuario.stream().map(oPersonal -> {
			PersonaDTO oPersonaDTO = personas.get(oPersonal.idPersonal());
			return new PersonalSinUsuarioDTO(oPersonal.idPersonal(), oPersonaDTO == null ? null : oPersonaDTO.nombres(),
					oPersonaDTO == null ? null : oPersonaDTO.apellidos(), oPersonal.cargo());
		}).toList();
	}

	@Transactional
	public UsuarioDTO registrar(UsuarioDTO oUsuarioDTO) {
		if (repositorioUsuario.existsByNombreUsuario(oUsuarioDTO.nombreUsuario())) {
			throw new ReglaNegocioException("RF81", "Ese nombre de usuario ya existe.", HttpStatus.CONFLICT);
		}
		if (personalApi.obtenerIdSede(oUsuarioDTO.idPersonal()).isEmpty()) {
			throw new ReglaNegocioException("RF81", "La persona " + oUsuarioDTO.idPersonal() + " no es del personal.");
		}
		String claveHash = codificadorClave.codificar(oUsuarioDTO.clave());
		Usuario oUsuario = new Usuario(oUsuarioDTO.idPersonal(), oUsuarioDTO.idRol(), oUsuarioDTO.nombreUsuario(), claveHash);
		return mapperUsuario.aDTO(repositorioUsuario.save(oUsuario));
	}

	/** [It. 2] CUS53: la lista de usuarios para asignar el rol (diferencia 14 del front-end). */
	@Transactional(readOnly = true)
	public List<UsuarioDTO> listarUsuarios() {
		return repositorioUsuario.findAll().stream().map(mapperUsuario::aDTO).toList();
	}

	/** [It. 2] CUS53: los permisos (módulo y acción) que existen, para armar los de un rol (diferencia 16 del front-end). */
	@Transactional(readOnly = true)
	public List<PermisoDTO> listarPermisos() {
		return mapperUsuario.aDTOs(repositorioPermiso.findAll());
	}

	/** [It. 2] RN48: no se cambia el rol de la última administradora activa (422). */
	@Transactional
	public UsuarioDTO asignarRol(Long idUsuario, Long idRol) {
		Usuario oUsuario = repositorioUsuario.findById(idUsuario)
			.orElseThrow(() -> new ReglaNegocioException("RF81", "El usuario " + idUsuario + " no existe.", HttpStatus.NOT_FOUND));
		boolean esAdministradora = repositorioRol.findById(oUsuario.getIdRol()).map(Rol::getNombre).filter(UsuarioActualSesion.ADMINISTRADORA::equals)
			.isPresent();
		if (esAdministradora && !oUsuario.getIdRol().equals(idRol) && !repositorioUsuario.existeOtraAdministradoraActiva(idUsuario)) {
			throw new ReglaNegocioException("RN48", "Siempre queda al menos una usuaria activa con el rol ADMINISTRADORA.");
		}
		oUsuario.asignarRol(idRol);
		return mapperUsuario.aDTO(repositorioUsuario.save(oUsuario));
	}

	/**
	 * [It. 2 · CUS54, RF82] La clave temporal se guarda solo como BCrypt; la cuenta queda con debe_cambiar_clave = 'S', sin intentos fallidos y
	 * desbloqueada (BLOQUEADO pasa a ACTIVO; una cuenta INACTIVA sigue así, porque la desactiva el retiro del personal). En su siguiente ingreso
	 * (CUS50) cambia la clave. La clave nunca vuelve en la respuesta.
	 */
	@Transactional
	public UsuarioDTO restablecerClave(Long idUsuario, String clave) {
		Usuario oUsuario = repositorioUsuario.findById(idUsuario)
			.orElseThrow(() -> new ReglaNegocioException("RF82", "El usuario " + idUsuario + " no existe.", HttpStatus.NOT_FOUND));
		String claveHash = codificadorClave.codificar(clave);
		oUsuario.restablecerClave(claveHash);
		return mapperUsuario.aDTO(repositorioUsuario.saveAndFlush(oUsuario));
	}

	/** Los permisos del pedido reemplazan a los del rol, porque es un PUT. [It. 2] RN48: ADMINISTRADORA conserva SEGURIDAD:VER, CREAR y EDITAR (422). */
	@Transactional
	public RolDTO configurarPermisos(Long idRol, RolDTO oRolDTO) {
		Rol oRol = repositorioRol.findById(idRol)
			.orElseThrow(() -> new ReglaNegocioException("RF78", "El rol " + idRol + " no existe.", HttpStatus.NOT_FOUND));
		Set<Long> pedidos = new LinkedHashSet<>();
		for (PermisoDTO permiso : oRolDTO.permisos()) {
			pedidos.add(repositorioPermiso.findOrCreate(permiso.modulo(), permiso.accion()).getIdPermiso());
		}
		oRol.actualizar(oRolDTO.descripcion(), oRolDTO.estado());
		oRol.getPermisos().stream()
			.map(RolPermiso::getIdPermiso)
			.filter(idPermiso -> !pedidos.contains(idPermiso))
			.toList()
			.forEach(oRol::revocarPermiso);
		pedidos.forEach(oRol::concederPermiso);
		oRol.verificarPermisosMinimos(ACCIONES_MINIMAS.stream().map(accion -> repositorioPermiso.findOrCreate(SEGURIDAD, accion).getIdPermiso()).toList());
		repositorioRol.saveAndFlush(oRol);
		return aDTO(oRol);
	}

	private RolDTO aDTO(Rol oRol) {
		return mapperUsuario.aDTO(oRol, repositorioUsuario.findPermisosPorRol(oRol.getIdRol()));
	}

}
