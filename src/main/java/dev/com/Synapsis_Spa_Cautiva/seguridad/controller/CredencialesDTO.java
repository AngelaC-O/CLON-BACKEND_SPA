package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import jakarta.validation.constraints.NotBlank;

public record CredencialesDTO(@NotBlank String nombreUsuario, @NotBlank String clave) {
}
