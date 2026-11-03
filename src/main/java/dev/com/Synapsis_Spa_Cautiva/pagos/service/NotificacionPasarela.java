package dev.com.Synapsis_Spa_Cautiva.pagos.service;

/** Lo único que el dominio recibe del webhook. estado: APROBADA o RECHAZADA. */
public record NotificacionPasarela(String codigoOperacion, String estado) {
}
