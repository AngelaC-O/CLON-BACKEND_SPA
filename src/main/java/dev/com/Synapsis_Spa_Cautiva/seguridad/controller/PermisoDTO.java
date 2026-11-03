package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermisoDTO(@NotBlank @Size(max = 40) String modulo, @NotBlank @Size(max = 30) String accion) {
}
