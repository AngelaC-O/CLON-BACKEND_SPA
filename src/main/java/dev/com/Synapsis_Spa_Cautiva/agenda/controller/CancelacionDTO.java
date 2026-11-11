package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS10, RF12] Cuerpo de PATCH /citas/{idCita}/cancelacion. Si la clienta no se presentó, el motivo es «No se presentó» (RN05). Sin
 * motivo responde 422 desde Cita.cancelar (CK_CITA_CANCELACION).
 */
public record CancelacionDTO(@Size(max = 200) String motivoCancelacion) {
}
