package dev.com.Synapsis_Spa_Cautiva.personal.api;

import java.time.LocalTime;

/** La especialista y su horario de un día. */
public record EspecialistaHorario(Long idPersonal, LocalTime horaEntrada, LocalTime horaSalida) {
}
