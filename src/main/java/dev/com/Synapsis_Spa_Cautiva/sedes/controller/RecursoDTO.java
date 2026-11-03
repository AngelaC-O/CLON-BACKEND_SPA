package dev.com.Synapsis_Spa_Cautiva.sedes.controller;

import dev.com.Synapsis_Spa_Cautiva.sedes.domain.EstadoRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.TipoRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecursoDTO(Long idRecurso, @NotNull Long idSede, @NotBlank @Size(max = 60) String nombre, @NotNull TipoRecurso tipo,
		@NotNull Integer minutosLimpieza, EstadoRecurso estado) {
}
