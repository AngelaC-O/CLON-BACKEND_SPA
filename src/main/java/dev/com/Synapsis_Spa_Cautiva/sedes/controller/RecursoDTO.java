package dev.com.Synapsis_Spa_Cautiva.sedes.controller;

import dev.com.Synapsis_Spa_Cautiva.sedes.domain.EstadoRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.TipoRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** [It. 2.1 · v5.2] Sin minutosLimpieza (RN03, RF85): si un cliente todavía lo envía, Jackson lo ignora. */
public record RecursoDTO(Long idRecurso, @NotNull Long idSede, @NotBlank @Size(max = 60) String nombre, @NotNull TipoRecurso tipo,
		EstadoRecurso estado) {
}
