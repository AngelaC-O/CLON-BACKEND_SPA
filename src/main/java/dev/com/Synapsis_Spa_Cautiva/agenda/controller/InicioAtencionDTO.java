package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InicioAtencionDTO(@NotNull Long idCita, @NotNull @Positive Integer tiempoDeclaradoMin) {
}
