package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.EstadoRol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RolDTO(Long idRol, String nombre, @Size(max = 150) String descripcion, @NotNull EstadoRol estado,
		@NotNull List<@Valid PermisoDTO> permisos) {
}
