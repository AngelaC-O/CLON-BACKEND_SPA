package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.personal.api.PersonalApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.MapperUsuario;
import dev.com.Synapsis_Spa_Cautiva.seguridad.controller.PermisoDTO;
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

	private final RepositorioUsuario repositorioUsuario;

	private final CodificadorClave codificadorClave;

	private final RepositorioRol repositorioRol;

	private final RepositorioPermiso repositorioPermiso;

	private final PersonalApi personalApi;

	private final MapperUsuario mapperUsuario;

	@Transactional(readOnly = true)
	public List<RolDTO> listarRoles() {
		return repositorioUsuario.findRolesActivos().stream().map(this::aDTO).toList();
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

	@Transactional
	public UsuarioDTO asignarRol(Long idUsuario, Long idRol) {
		Usuario oUsuario = repositorioUsuario.findById(idUsuario)
			.orElseThrow(() -> new ReglaNegocioException("RF81", "El usuario " + idUsuario + " no existe.", HttpStatus.NOT_FOUND));
		oUsuario.asignarRol(idRol);
		return mapperUsuario.aDTO(repositorioUsuario.save(oUsuario));
	}

	/** Los permisos del pedido reemplazan a los del rol, porque es un PUT. */
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
		repositorioRol.saveAndFlush(oRol);
		return aDTO(oRol);
	}

	private RolDTO aDTO(Rol oRol) {
		return mapperUsuario.aDTO(oRol, repositorioUsuario.findPermisosPorRol(oRol.getIdRol()));
	}

}
