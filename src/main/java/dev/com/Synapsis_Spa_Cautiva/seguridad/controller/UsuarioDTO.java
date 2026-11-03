package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.EstadoUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** La clave solo se recibe: nunca sale en una respuesta. */
public record UsuarioDTO(Long idUsuario, @NotNull Long idPersonal, @NotNull Long idRol, @NotBlank @Size(max = 50) String nombreUsuario,
		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @NotBlank @Size(min = 8, max = 72) String clave, EstadoUsuario estado,
		Boolean debeCambiarClave) {
}
