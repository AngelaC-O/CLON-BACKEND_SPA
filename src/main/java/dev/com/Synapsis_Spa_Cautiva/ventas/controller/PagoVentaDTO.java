package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;

public record PagoVentaDTO(Long idPago, Long idMedioPago, BigDecimal monto, Long idTransaccion, String referencia) {
}
