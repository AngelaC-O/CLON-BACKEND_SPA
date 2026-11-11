package dev.com.Synapsis_Spa_Cautiva.pagos.service;

/** Puerto de Izipay (ADR-09). Nunca recibe datos de tarjeta (RN43). */
public interface PasarelaPagosPort {

	RespuestaPasarela crearCobro(SolicitudCobro oSolicitudCobro);

	RespuestaPasarela crearEnlace(SolicitudCobro oSolicitudCobro);

	/** [It. 2 · CUS24, RN44] Reembolso de un cobro con tarjeta. estado: APROBADA, RECHAZADA o PENDIENTE (si la pasarela no respondió). */
	RespuestaPasarela devolver(SolicitudDevolucion oSolicitudDevolucion);

	/** Valida la firma y traduce el cuerpo que define Izipay. Responde 401 si la firma no es válida. */
	NotificacionPasarela traducirNotificacion(String cuerpo, String firmaHmac);

}
