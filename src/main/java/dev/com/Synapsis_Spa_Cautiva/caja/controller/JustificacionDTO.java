package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoConciliacionItem;
import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS47, RF75] Cuerpo del PATCH de un ítem: JUSTIFICADA (con su justificación, CK_CONC_ITEM_JUSTIFICADA) o EXCLUIDA. El diagrama dice
 * EstadoConciliacion, pero ese es el de la conciliación (ABIERTA o CERRADA): se usa EstadoConciliacionItem (DF-70).
 */
public record JustificacionDTO(EstadoConciliacionItem estado, @Size(max = 300) String justificacion) {
}
