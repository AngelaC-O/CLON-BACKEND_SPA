package dev.com.Synapsis_Spa_Cautiva.pagos.service;

/** estado: APROBADA, RECHAZADA o PENDIENTE. enlacePago solo viene al crear un enlace. */
public record RespuestaPasarela(String estado, String codigoOperacion, String enlacePago, String mensaje) {
}
