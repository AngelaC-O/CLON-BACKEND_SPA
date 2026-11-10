package dev.com.Synapsis_Spa_Cautiva.pagos.controller;

import dev.com.Synapsis_Spa_Cautiva.pagos.domain.EstadoMedioPago;

/**
 * [It. 2 · CUS62, RF88] Un medio de pago. En el PATCH solo van estado y usaPasarela; el efectivo nunca usa la pasarela (422, RN42,
 * CK_MEDIO_PAGO_EFECTIVO). Reemplaza la copia fija de los id 1 a 4 del front-end (Duda 11).
 */
public record MedioPagoDTO(Long idMedioPago, String nombre, EstadoMedioPago estado, Boolean usaPasarela) {
}
