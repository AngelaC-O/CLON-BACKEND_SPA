package dev.com.Synapsis_Spa_Cautiva.pagos.api;

/**
 * estado y canal: los de TRANSACCION_PASARELA, como texto porque los enums del dominio son internos. [It. 2] canal (POS, QR, LINK o API) se
 * agrega al final: CUS24 devuelve por la pasarela solo lo que no se cobró por QR (RN44).
 */
public record TransaccionDTO(Long idTransaccion, String estado, String codigoOperacion, String enlacePago, String canal) {

	private static final String APROBADA = "APROBADA";

	private static final String QR = "QR";

	public boolean aprobada() {
		return APROBADA.equals(estado);
	}

	/** [It. 2 · RN44] Yape y Plin pagados por QR los devuelve la Administradora desde la app (devolución manual). */
	public boolean porQr() {
		return QR.equals(canal);
	}

}
