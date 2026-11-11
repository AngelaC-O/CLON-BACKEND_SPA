package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import jakarta.validation.constraints.Size;

/** [It. 2 · CUS23, RN32] Cuerpo del rechazo. Sin motivo responde 422 desde SolicitudAnulacion.rechazar (CK_SOL_ANUL_RECHAZO). */
public record RechazoDTO(@Size(max = 300) String motivoRechazo) {
}
