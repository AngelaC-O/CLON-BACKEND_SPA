package dev.com.Synapsis_Spa_Cautiva.pagos.api;

import java.time.LocalDateTime;

public record EnlacePagoDTO(Long idTransaccion, String estado, String canal, String enlacePago, LocalDateTime fechaExpiracion) {
}
