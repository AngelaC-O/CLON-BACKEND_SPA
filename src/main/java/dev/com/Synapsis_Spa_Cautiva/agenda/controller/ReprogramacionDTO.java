package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

/** [It. 2 · CUS09, RF12] La nueva fecha y hora, con la especialista y el recurso; la duración es la de la cita origen. */
public record ReprogramacionDTO(@NotNull LocalDateTime fechaHoraInicio, @NotNull Long idPersonal, @NotNull Long idRecurso) {
}
