package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;

/**
 * APISPERU consulta DNI/RUC, con límite de tiempo y sin reintentos: si falla, la ficha se ingresa a mano y queda MANUAL (RN45). El token va en la
 * URL, así que el log solo lleva el tipo de error: el mensaje de la excepción la incluye.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cautiva.integraciones.apisperu.modo", havingValue = "real")
public class AdaptadorApisPeruConsulta implements ConsultaDocumentoPort {

	private static final String CODIGO_DNI = "1";

	private static final String CODIGO_RUC = "6";

	private static final String ENCONTRADO = "ENCONTRADO";

	private static final String NO_ENCONTRADO = "NO_ENCONTRADO";

	private static final String ERROR = "ERROR";

	private static final int HTTP_NO_ENCONTRADO = 404;

	private final RestClient restClient;

	private final String token;

	public AdaptadorApisPeruConsulta(@Value("${cautiva.integraciones.apisperu.consulta-url}") String url,
			@Value("${cautiva.integraciones.apisperu.token}") String token,
			@Value("${cautiva.integraciones.apisperu.consulta-limite:3s}") Duration limite) {
		HttpClient cliente = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(limite).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
		fabrica.setReadTimeout(limite);
		this.restClient = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
		this.token = token;
	}

	@Override
	public DatosPersona consultar(String codigoSunat, String numeroDocumento) {
		try {
			return switch (codigoSunat) {
				case CODIGO_DNI -> consultarDni(numeroDocumento);
				case CODIGO_RUC -> consultarRuc(numeroDocumento);
				default -> error();
			};
		}
		catch (RestClientResponseException ex) {
			return respuestaDeError(ex.getStatusCode());
		}
		catch (RestClientException ex) {
			log.warn("La consulta de documento falló: {}", ex.getClass().getSimpleName());
			return error();
		}
	}

	private DatosPersona consultarDni(String dni) {
		RespuestaDni respuesta = restClient.get().uri("/dni/{dni}?token={token}", dni, token).retrieve().body(RespuestaDni.class);
		if (respuesta == null || respuesta.nombres() == null || respuesta.nombres().isBlank()) {
			return new DatosPersona(NO_ENCONTRADO, null, null, null);
		}
		String apellidos = (texto(respuesta.apellidoPaterno()) + " " + texto(respuesta.apellidoMaterno())).trim();
		return new DatosPersona(ENCONTRADO, respuesta.nombres(), apellidos, null);
	}

	private DatosPersona consultarRuc(String ruc) {
		RespuestaRuc respuesta = restClient.get().uri("/ruc/{ruc}?token={token}", ruc, token).retrieve().body(RespuestaRuc.class);
		if (respuesta == null || respuesta.razonSocial() == null || respuesta.razonSocial().isBlank()) {
			return new DatosPersona(NO_ENCONTRADO, null, null, null);
		}
		return new DatosPersona(ENCONTRADO, null, null, respuesta.razonSocial());
	}

	private DatosPersona respuestaDeError(HttpStatusCode estado) {
		if (estado.value() == HTTP_NO_ENCONTRADO) {
			return new DatosPersona(NO_ENCONTRADO, null, null, null);
		}
		log.warn("La consulta de documento respondió {}", estado.value());
		return error();
	}

	private static DatosPersona error() {
		return new DatosPersona(ERROR, null, null, null);
	}

	private static String texto(String valor) {
		return valor == null ? "" : valor;
	}

	private record RespuestaDni(String nombres, String apellidoPaterno, String apellidoMaterno) {
	}

	private record RespuestaRuc(String razonSocial) {
	}

}
