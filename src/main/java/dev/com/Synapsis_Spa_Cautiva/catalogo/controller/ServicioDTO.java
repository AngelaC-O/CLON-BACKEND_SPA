package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoServicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoRecurso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServicioDTO(Long idServicio, @NotNull Long idArea, @NotBlank @Size(max = 80) String nombre, @NotNull Integer duracionRefMin,
		@NotNull TipoRecurso tipoRecurso, EstadoServicio estado, @NotEmpty List<@Valid VarianteDTO> variantes) {
}
