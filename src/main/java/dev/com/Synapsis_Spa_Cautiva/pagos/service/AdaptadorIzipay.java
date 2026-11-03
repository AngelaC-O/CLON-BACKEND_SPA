package dev.com.Synapsis_Spa_Cautiva.pagos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Izipay por la API REST V4 de micuentaweb (contrato sin verificar con el proveedor: DF-49). El cobro y el enlace son una orden de pago: la clienta
 * paga en la página de Izipay, por la URL o su QR, y este sistema nunca ve la tarjeta (RN43). Por eso nacen PENDIENTE y los aprueba el webhook. El
 * código de operación es el orderId que se genera aquí: se conoce antes de pagar y es por el que llega la notificación.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cautiva.integraciones.izipay.modo", havingValue = "real")
public class AdaptadorIzipay implements PasarelaPagosPort {

	private static final String RUTA_ORDEN = "/api-payment/V4/Charge/CreatePaymentOrder";

	private static final String MONEDA = "PEN";

	private static final String APROBADA = "APROBADA";

	private static final String RECHAZADA = "RECHAZADA";

	private static final String PENDIENTE = "PENDIENTE";

	private static final String RESPUESTA_CORRECTA = "SUCCESS";

	private static final String PAGADA = "PAID";

	private static final String IMPAGA = "UNPAID";

	private static final String CICLO_CERRADO = "CLOSED";

	private static final String HMAC = "HmacSHA256";

	private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final RestClient restClient;

	private final String claveHmac;

	public AdaptadorIzipay(@Value("${cautiva.integraciones.izipay.api-url}") String url, @Value("${cautiva.integraciones.izipay.usuario}") String usuario,
			@Value("${cautiva.integraciones.izipay.clave}") String clave, @Value("${cautiva.integraciones.izipay.hmac-clave}") String claveHmac,
			@Value("${cautiva.integraciones.izipay.limite:10s}") Duration limite) {
		HttpClient cliente = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(limite).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
		fabrica.setReadTimeout(limite);
		String credenciales = Base64.getEncoder().encodeToString((usuario + ":" + clave).getBytes(StandardCharsets.UTF_8));
		this.restClient = RestClient.builder()
			.baseUrl(url)
			.requestFactory(fabrica)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credenciales)
			.build();
		this.claveHmac = claveHmac;
	}

	@Override
	public RespuestaPasarela crearCobro(SolicitudCobro oSolicitudCobro) {
		return crearOrden(oSolicitudCobro);
	}

	@Override
	public RespuestaPasarela crearEnlace(SolicitudCobro oSolicitudCobro) {
		return crearOrden(oSolicitudCobro);
	}

	/** Si Izipay no responde, queda PENDIENTE sin URL: no se sabe si la orden se creó, y ServicioPago decide. */
	private RespuestaPasarela crearOrden(SolicitudCobro oSolicitudCobro) {
		String codigo = "CS-" + UUID.randomUUID().toString().replace("-", "");
		try {
			RespuestaOrden respuesta = restClient.post()
				.uri(RUTA_ORDEN)
				.contentType(MediaType.APPLICATION_JSON)
				.body(new PeticionOrden(centavos(oSolicitudCobro.monto()), MONEDA, codigo, enFormatoIzipay(oSolicitudCobro.fechaExpiracion()), new Canal("URL")))
				.retrieve()
				.body(RespuestaOrden.class);
			if (respuesta != null && RESPUESTA_CORRECTA.equals(respuesta.status()) && respuesta.answer() != null) {
				return new RespuestaPasarela(PENDIENTE, codigo, respuesta.answer().paymentURL(), null);
			}
			String mensaje = respuesta == null || respuesta.answer() == null ? "Respuesta vacía de la pasarela" : respuesta.answer().errorMessage();
			return new RespuestaPasarela(RECHAZADA, codigo, null, mensaje);
		}
		catch (RestClientException ex) {
			log.warn("Izipay no respondió la orden {}: {}", codigo, ex.getClass().getSimpleName());
			return new RespuestaPasarela(PENDIENTE, codigo, null, "La pasarela no respondió");
		}
	}

	/** La IPN llega como formulario con kr-answer y kr-hash: HMAC-SHA-256 en hexadecimal de kr-answer. La cabecera no se usa. */
	@Override
	public NotificacionPasarela traducirNotificacion(String cuerpo, String firmaHmac) {
		Map<String, String> campos = leerFormulario(cuerpo);
		String respuesta = campos.get("kr-answer");
		String firma = campos.get("kr-hash");
		if (claveHmac.isBlank() || respuesta == null || firma == null || !firmaValida(respuesta, firma)) {
			log.warn("Webhook de Izipay rechazado: firma no válida");
			throw new ReglaNegocioException("RN42", "La firma de la notificación no es válida.", HttpStatus.UNAUTHORIZED);
		}
		try {
			RespuestaIpn oIpn = JSON.readValue(respuesta, RespuestaIpn.class);
			return new NotificacionPasarela(oIpn.orderDetails().orderId(), estadoDe(oIpn));
		}
		catch (JacksonException | NullPointerException ex) {
			throw new ReglaNegocioException("RN42", "La notificación no se puede leer.", HttpStatus.BAD_REQUEST);
		}
	}

	/** Un intento fallido con la orden abierta aún se puede volver a pagar: solo es RECHAZADA cuando la orden se cerró sin pago. */
	private static String estadoDe(RespuestaIpn oIpn) {
		if (PAGADA.equals(oIpn.orderStatus())) {
			return APROBADA;
		}
		return IMPAGA.equals(oIpn.orderStatus()) && CICLO_CERRADO.equals(oIpn.orderCycle()) ? RECHAZADA : PENDIENTE;
	}

	private boolean firmaValida(String respuesta, String firma) {
		try {
			Mac mac = Mac.getInstance(HMAC);
			mac.init(new SecretKeySpec(claveHmac.getBytes(StandardCharsets.UTF_8), HMAC));
			String esperada = HexFormat.of().formatHex(mac.doFinal(respuesta.getBytes(StandardCharsets.UTF_8)));
			return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8), firma.getBytes(StandardCharsets.UTF_8));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("No se pudo calcular la firma", ex);
		}
	}

	private static Map<String, String> leerFormulario(String cuerpo) {
		Map<String, String> campos = new HashMap<>();
		for (String par : cuerpo.split("&")) {
			int separador = par.indexOf('=');
			if (separador > 0) {
				campos.put(URLDecoder.decode(par.substring(0, separador), StandardCharsets.UTF_8),
						URLDecoder.decode(par.substring(separador + 1), StandardCharsets.UTF_8));
			}
		}
		return campos;
	}

	private static long centavos(BigDecimal monto) {
		return monto.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
	}

	private static String enFormatoIzipay(LocalDateTime fecha) {
		return fecha == null ? null : fecha.atZone(ZONA_LIMA).toOffsetDateTime().toString();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record PeticionOrden(long amount, String currency, String orderId, String expirationDate, Canal channelOptions) {
	}

	private record Canal(String channelType) {
	}

	private record RespuestaOrden(String status, DatosOrden answer) {
	}

	private record DatosOrden(String paymentURL, String errorMessage) {
	}

	private record RespuestaIpn(String orderStatus, String orderCycle, DetalleOrden orderDetails) {
	}

	private record DetalleOrden(String orderId) {
	}

}
