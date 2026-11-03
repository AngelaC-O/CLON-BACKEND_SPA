package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.LineaNegocio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AreaDTO(Long idArea, @NotNull LineaNegocio linea, @NotBlank @Size(max = 60) String nombreArea) {
}
