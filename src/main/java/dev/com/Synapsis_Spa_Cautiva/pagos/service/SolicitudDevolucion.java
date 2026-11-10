package dev.com.Synapsis_Spa_Cautiva.pagos.service;

import java.math.BigDecimal;

/**
 * [It. 2 · CUS24, RN44] Lo que se le pide a la pasarela para devolver un cobro: el código de operación con que se cobró, el medio y el monto. Nunca
 * lleva datos de tarjeta (RN43).
 */
public record SolicitudDevolucion(String codigoOperacionOrigen, Long idMedioPago, BigDecimal monto) {
}
