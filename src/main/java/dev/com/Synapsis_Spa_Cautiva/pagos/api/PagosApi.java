package dev.com.Synapsis_Spa_Cautiva.pagos.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public interface PagosApi {

	/** El efectivo se cobra y se cuenta en caja: no pasa por cobrar (RN42). */
	boolean usaPasarela(Long idMedioPago);

	/** canal: POS, QR o API. Una respuesta RECHAZADA responde 422 (RF103); PENDIENTE no respalda ningún pago (RN42). */
	TransaccionDTO cobrar(Long idVenta, Long idMedioPago, BigDecimal monto, String canal);

	/** El adelanto no se registra aquí: lo hace agenda cuando llega AdelantoAprobado (RN04). */
	EnlacePagoDTO generarEnlace(Long idCita, BigDecimal adelantoMonto, Long idMedioPago, Long idSede);

	/** Cobros APROBADOS del día por idMedioPago (RF64, RN42). */
	Map<Long, BigDecimal> aprobadosDelDia(Long idSede, LocalDateTime fecha);

}
