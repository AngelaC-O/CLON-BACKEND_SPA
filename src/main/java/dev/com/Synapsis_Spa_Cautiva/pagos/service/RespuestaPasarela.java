package dev.com.Synapsis_Spa_Cautiva.pagos.service;

/** estado: APROBADA, RECHAZADA o PENDIENTE. enlacePago: la URL que se comparte o se muestra como QR; vacía si la pasarela no respondió. */
public record RespuestaPasarela(String estado, String codigoOperacion, String enlacePago, String mensaje) {
}
