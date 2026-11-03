package dev.com.Synapsis_Spa_Cautiva.ventas.api;

import java.time.LocalDate;

public record ComprobantePendiente(Long idComprobante, String tipo, String serie, String numero, String estadoSunat, LocalDate fechaLimite) {
}
