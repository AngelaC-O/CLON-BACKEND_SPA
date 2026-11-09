package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionCobrable;

/**
 * [It. 2] CUS18: lo que la recepcionista tiene por cobrar en su sede. atenciones: FINALIZADAS que aún no están en una venta (RF21);
 * ventasAbiertas: las ventas ABIERTAS para retomarlas, por ejemplo con un cobro digital PENDIENTE (RN52).
 */
public record PorCobrarDTO(List<AtencionCobrable> atenciones, List<VentaDTO> ventasAbiertas) {
}
