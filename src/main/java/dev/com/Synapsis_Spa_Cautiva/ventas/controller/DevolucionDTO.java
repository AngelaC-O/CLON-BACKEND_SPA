package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.FormaDevolucion;

/**
 * [It. 2 · CUS24, RF106, RN44] Petición y respuesta. En POST /ventas/{idVenta}/devolucion se manda formaDevolucion (MEDIO_ORIGINAL); en
 * /devolucion-manual, idTransaccion (el cobro por QR) y codigoOperacion (el número de la app). La respuesta trae la nota de crédito y las
 * DEVOLUCIONES creadas en TRANSACCION_PASARELA.
 */
public record DevolucionDTO(Long idVenta, FormaDevolucion formaDevolucion, Long idTransaccion, String codigoOperacion, Long idNotaCredito,
		List<TransaccionDTO> devoluciones) {
}
