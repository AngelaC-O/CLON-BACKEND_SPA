package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Firma el enlace público con su vencimiento (RF102): cambiar el comprobante o la fecha invalida la firma. Sin clave no hay enlaces. */
@Component
class FirmaEnlaceComprobante {

	private static final String HMAC = "HmacSHA256";

	private final String clave;

	FirmaEnlaceComprobante(@Value("${cautiva.comprobantes.enlace-clave}") String clave) {
		this.clave = clave;
	}

	String firmar(Long idComprobante, long expira) {
		if (clave.isBlank()) {
			throw new ReglaNegocioException("RF102", "Falta configurar la clave de los enlaces públicos.", HttpStatus.SERVICE_UNAVAILABLE);
		}
		return calcular(idComprobante + ":" + expira);
	}

	boolean esValida(Long idComprobante, long expira, String firma) {
		if (clave.isBlank() || Instant.now().getEpochSecond() > expira) {
			return false;
		}
		return MessageDigest.isEqual(firmar(idComprobante, expira).getBytes(StandardCharsets.UTF_8), firma.getBytes(StandardCharsets.UTF_8));
	}

	private String calcular(String datos) {
		try {
			Mac mac = Mac.getInstance(HMAC);
			mac.init(new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), HMAC));
			return HexFormat.of().formatHex(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("No se pudo calcular la firma", ex);
		}
	}

}
