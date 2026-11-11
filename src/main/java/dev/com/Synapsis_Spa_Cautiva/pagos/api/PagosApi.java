package dev.com.Synapsis_Spa_Cautiva.pagos.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PagosApi {

	/** El efectivo se cobra y se cuenta en caja: no pasa por cobrar (RN42). */
	boolean usaPasarela(Long idMedioPago);

	/**
	 * canal: POS, QR, LINK o API. Una respuesta RECHAZADA responde 422 (RF103); PENDIENTE no respalda ningún pago (RN42). [It. 2 · RN52] La
	 * transacción se guarda en su propia transacción de base: APROBADA, RECHAZADA o PENDIENTE, queda aunque la venta haga rollback.
	 */
	TransaccionDTO cobrar(Long idVenta, Long idMedioPago, BigDecimal monto, String canal);

	/** El cobro que quedó PENDIENTE y el webhook ya aprobó: 422 si aún no lo está (RN42). */
	void exigirAprobada(Long idTransaccion);

	/** [It. 2] CUS18: el estado actual de una transacción (RN42, RN52); 404 si no existe. */
	TransaccionDTO consultarTransaccion(Long idTransaccion);

	/** El adelanto no se registra aquí: lo hace agenda cuando llega AdelantoAprobado (RN04). */
	EnlacePagoDTO generarEnlace(Long idCita, BigDecimal adelantoMonto, Long idMedioPago, Long idSede);

	/**
	 * [It. 2 · CUS24, RF106, RN44] Devuelve por la pasarela el cobro APROBADO idTransaccion: otra TRANSACCION_PASARELA DEVOLUCION por el mismo
	 * monto, con id_transaccion_origen e id_nota_credito. Solo para lo que no se cobró por QR (422). Se guarda en su propia transacción de base,
	 * como el cobro: lo que la pasarela devolvió queda aunque la venta haga rollback; una RECHAZADA se guarda y responde 422.
	 */
	TransaccionDTO devolver(Long idTransaccion, Long idNotaCredito);

	/**
	 * [It. 2 · CUS24, RF106, RN44] Yape o Plin pagados por QR: la Administradora ya devolvió desde la app y aquí queda la DEVOLUCION APROBADA, con
	 * canal QR y el número de operación de la app.
	 */
	TransaccionDTO registrarDevolucionManual(Long idTransaccion, Long idNotaCredito, String codigoOperacion);

	/**
	 * [It. 2 · CUS47, RF105] Completa la comisión y la fecha de abono de cada cobro APROBADO por su codigo_operacion, con el reporte de liquidación
	 * que la Administradora descargó de Izipay (no hay API de liquidación, DF-49). Las filas que no casan solo se informan.
	 */
	ResumenImportacionDTO registrarLiquidacion(List<FilaLiquidacionDTO> filasLiquidacion);

	/** [It. 2 · CUS47, RF105] Las transacciones APROBADAS del mes con su comisión y su abono, para conciliar (RN31). */
	List<TransaccionLiquidadaDTO> liquidacion(Integer anio, Integer mes);

	/** Cobros APROBADOS del día por idMedioPago (RF64, RN42). */
	Map<Long, BigDecimal> aprobadosDelDia(Long idSede, LocalDateTime fecha);

}
