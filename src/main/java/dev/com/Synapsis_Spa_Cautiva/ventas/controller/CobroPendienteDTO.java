package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

/** enlacePago: la URL que la clienta paga o escanea como QR; vacía si la pasarela no respondió. */
public record CobroPendienteDTO(Long idTransaccion, String enlacePago) {
}
