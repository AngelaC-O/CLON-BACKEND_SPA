package dev.com.Synapsis_Spa_Cautiva.personal.api;

import java.time.LocalTime;

/**
 * La especialista y su horario de un día. [It. 2] cargo: el de PERSONAL, como texto porque el enum del dominio es interno. Sin un día
 * (listarPersonal, listarEspecialistas de una sede), horaEntrada y horaSalida vienen vacías.
 */
public record EspecialistaHorario(Long idPersonal, LocalTime horaEntrada, LocalTime horaSalida, String cargo) {
}
