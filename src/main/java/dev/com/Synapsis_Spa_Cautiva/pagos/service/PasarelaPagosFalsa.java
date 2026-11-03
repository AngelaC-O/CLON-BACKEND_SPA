package dev.com.Synapsis_Spa_Cautiva.pagos.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sin llamadas externas hasta B5. El cobro responde lo que diga la propiedad: APROBADA en dev y PENDIENTE si no se configura, para no
 * contar dinero que nadie cobró. El enlace siempre queda PENDIENTE hasta el webhook. Sin clave configurada, ninguna firma es válida.
 */
@Component
class PasarelaPagosFalsa implements PasarelaPagosPort {

	private static final String PENDIENTE = "PENDIENTE";

	private static final String HMAC = "HmacSHA256";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final String respuestaFalsa;

	private final String claveHmac;

	PasarelaPagosFalsa(@Value("${cautiva.integraciones.izipay.respuesta-falsa:PENDIENTE}") String respuestaFalsa,
			@Value("${cautiva.integraciones.izipay.hmac-clave:}") String claveHmac) {
		this.respuestaFalsa = respuestaFalsa;
		this.claveHmac = claveHmac;
	}

	@Override
	public RespuestaPasarela crearCobro(SolicitudCobro oSolicitudCobro) {
		return new RespuestaPasarela(respuestaFalsa, nuevoCodigo(), null, "Respuesta del adaptador falso");
	}

	@Override
	public RespuestaPasarela crearEnlace(SolicitudCobro oSolicitudCobro) {
		String codigo = nuevoCodigo();
		return new RespuestaPasarela(PENDIENTE, codigo, "https://pasarela-falsa.invalid/enlace/" + codigo, "Respuesta del adaptador falso");
	}

	@Override
	public NotificacionPasarela traducirNotificacion(String cuerpo, String firmaHmac) {
		if (claveHmac.isBlank() || firmaHmac == null || !firmaValida(cuerpo, firmaHmac)) {
			throw new ReglaNegocioException("RN42", "La firma de la notificación no es válida.", HttpStatus.UNAUTHORIZED);
		}
		try {
			return JSON.readValue(cuerpo, NotificacionPasarela.class);
		}
		catch (JacksonException ex) {
			throw new ReglaNegocioException("RN42", "La notificación no se puede leer.", HttpStatus.BAD_REQUEST);
		}
	}

	private boolean firmaValida(String cuerpo, String firmaHmac) {
		try {
			Mac mac = Mac.getInstance(HMAC);
			mac.init(new SecretKeySpec(claveHmac.getBytes(StandardCharsets.UTF_8), HMAC));
			String esperada = HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
			return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8), firmaHmac.getBytes(StandardCharsets.UTF_8));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("No se pudo calcular la firma", ex);
		}
	}

	private static String nuevoCodigo() {
		return "FALSO-" + UUID.randomUUID();
	}

}
