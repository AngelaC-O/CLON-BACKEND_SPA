package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.EstadoUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * La clave solo se recibe: nunca sale en una respuesta. [It. 2.1 · nombres] nombres, apellidos (de la persona), nombreRol y ultimoAcceso solo
 * van en las respuestas; en la petición se ignoran.
 */
public record UsuarioDTO(Long idUsuario, @NotNull Long idPersonal, @NotNull Long idRol, @NotBlank @Size(max = 50) String nombreUsuario,
		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 8, max = 72) String clave, EstadoUsuario estado,
		Boolean debeCambiarClave, String nombres, String apellidos, String nombreRol, LocalDateTime ultimoAcceso) {

	public UsuarioDTO conNombres(String nombres, String apellidos, String nombreRol) {
		return new UsuarioDTO(idUsuario, idPersonal, idRol, nombreUsuario, clave, estado, debeCambiarClave, nombres, apellidos, nombreRol, ultimoAcceso);
	}

}
