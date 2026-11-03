package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioClaveDTO(@NotBlank String claveActual, @NotBlank @Size(min = 8, max = 72) String claveNueva) {
}
