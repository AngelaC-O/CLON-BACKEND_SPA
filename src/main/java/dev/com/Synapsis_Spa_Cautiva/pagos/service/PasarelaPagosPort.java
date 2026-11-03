package dev.com.Synapsis_Spa_Cautiva.pagos.service;

/** Puerto de Izipay (ADR-09). El adaptador real, AdaptadorIzipay, llega en B5. Nunca recibe datos de tarjeta (RN43). */
public interface PasarelaPagosPort {

	RespuestaPasarela crearCobro(SolicitudCobro oSolicitudCobro);

	RespuestaPasarela crearEnlace(SolicitudCobro oSolicitudCobro);

	/** Valida la firma y traduce el cuerpo que define Izipay. Responde 401 si la firma no es válida. */
	NotificacionPasarela traducirNotificacion(String cuerpo, String firmaHmac);

}
