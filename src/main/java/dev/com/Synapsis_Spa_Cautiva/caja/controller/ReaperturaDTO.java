package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import jakarta.validation.constraints.Size;

/** [It. 2 · CUS46, RF66, RN30] Cuerpo de la reapertura. Sin motivo responde 422 desde Caja.reabrir (Oracle lo repite con -20060). */
public record ReaperturaDTO(@Size(max = 300) String motivoReapertura) {
}
