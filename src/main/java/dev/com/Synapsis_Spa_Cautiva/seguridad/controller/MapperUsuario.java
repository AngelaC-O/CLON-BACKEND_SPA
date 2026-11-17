package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Permiso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Rol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Usuario;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapperUsuario {

	@Mapping(target = "clave", ignore = true)
	@Mapping(target = "nombres", ignore = true)
	@Mapping(target = "apellidos", ignore = true)
	@Mapping(target = "nombreRol", ignore = true)
	UsuarioDTO aDTO(Usuario oUsuario);

	PermisoDTO aDTO(Permiso oPermiso);

	List<PermisoDTO> aDTOs(List<Permiso> permisos);

	default RolDTO aDTO(Rol oRol, List<Permiso> permisos) {
		return new RolDTO(oRol.getIdRol(), oRol.getNombre(), oRol.getDescripcion(), oRol.getEstado(), aDTOs(permisos));
	}

}
