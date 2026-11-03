package dev.com.Synapsis_Spa_Cautiva.agenda.api;

import java.math.BigDecimal;

/** Solo el uso: el abono de una venta anulada lo registra Oracle y Java nunca lo hace (-20063). */
public interface SaldoFavorApi {

	void usarSaldo(Long idCliente, BigDecimal saldoFavorAplicado, Long idVenta);

}
