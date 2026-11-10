package dev.com.Synapsis_Spa_Cautiva;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.AdaptadorIzipay;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.NotificacionPasarela;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.RespuestaPasarela;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.SolicitudCobro;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.SolicitudDevolucion;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.AdaptadorApisPeruConsulta;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.DatosPersona;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.AdaptadorApisPeruFacturacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.DocumentoElectronico;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.RespuestaCpe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Los tres adaptadores contra WireMock, sin Oracle ni Spring: respuesta correcta, error y tiempo agotado. Prueban el contrato de DF-49, no al proveedor.
 */
class AdaptadoresExternosTest {

	private static final Duration LIMITE = Duration.ofMillis(300);

	private static final int DEMORA_MS = 1500;

	private static final String CLAVE_HMAC = "clave-de-prueba";

	private static final String RUTA_ORDEN = "/api-payment/V4/Charge/CreatePaymentOrder";

	private static final WireMockServer SERVIDOR = new WireMockServer(WireMockConfiguration.options().dynamicPort());

	private AdaptadorApisPeruConsulta consulta;

	private AdaptadorApisPeruFacturacion facturacion;

	private AdaptadorIzipay izipay;

	@BeforeAll
	static void arrancar() {
		SERVIDOR.start();
	}

	@AfterAll
	static void detener() {
		SERVIDOR.stop();
	}

	@BeforeEach
	void preparar() {
		SERVIDOR.resetAll();
		String url = SERVIDOR.baseUrl();
		consulta = new AdaptadorApisPeruConsulta(url, "token-consulta", LIMITE);
		facturacion = new AdaptadorApisPeruFacturacion(url, "token-facturacion", LIMITE, "20601234567", "CAUTIVA SPA E.I.R.L.", "Pichanaki", "120604", "JUNIN", "CHANCHAMAYO",
				"PICHANAKI");
		izipay = new AdaptadorIzipay(url, "usuario", "clave", CLAVE_HMAC, LIMITE);
	}

	// ------------------------------------------------------------------ consulta DNI y RUC

	@Test
	void consultaUnDniYDevuelveNombresYApellidos() {
		SERVIDOR.stubFor(get(urlPathEqualTo("/dni/71234567")).withQueryParam("token", equalTo("token-consulta"))
			.willReturn(okJson("{\"success\":true,\"dni\":\"71234567\",\"nombres\":\"ANA\",\"apellidoPaterno\":\"QUISPE\",\"apellidoMaterno\":\"ROJAS\"}")));

		assertThat(consulta.consultar("1", "71234567")).isEqualTo(new DatosPersona("ENCONTRADO", "ANA", "QUISPE ROJAS", null));
	}

	@Test
	void consultaUnRucYDevuelveLaRazonSocial() {
		SERVIDOR.stubFor(get(urlPathEqualTo("/ruc/20601234567")).willReturn(okJson("{\"ruc\":\"20601234567\",\"razonSocial\":\"EMPRESA DEMO S.A.C.\"}")));

		assertThat(consulta.consultar("6", "20601234567")).isEqualTo(new DatosPersona("ENCONTRADO", null, null, "EMPRESA DEMO S.A.C."));
	}

	@Test
	void unDocumentoQueNoExisteEsNoEncontrado() {
		SERVIDOR.stubFor(get(urlPathEqualTo("/dni/71234567")).willReturn(okJson("{\"success\":false,\"message\":\"No se encontraron resultados.\"}")));
		SERVIDOR.stubFor(get(urlPathEqualTo("/ruc/20601234567")).willReturn(aResponse().withStatus(404)));

		assertThat(consulta.consultar("1", "71234567").resultado()).isEqualTo("NO_ENCONTRADO");
		assertThat(consulta.consultar("6", "20601234567").resultado()).isEqualTo("NO_ENCONTRADO");
	}

	@Test
	void unErrorOTiempoAgotadoEsErrorYNoSeReintenta() {
		SERVIDOR.stubFor(get(urlPathEqualTo("/dni/71234567")).willReturn(serverError()));
		assertThat(consulta.consultar("1", "71234567").resultado()).isEqualTo("ERROR");
		SERVIDOR.verify(1, anyRequestedFor(anyUrl()));

		SERVIDOR.resetAll();
		SERVIDOR.stubFor(get(urlPathEqualTo("/dni/71234567")).willReturn(okJson("{\"nombres\":\"ANA\"}").withFixedDelay(DEMORA_MS)));
		assertThat(consulta.consultar("1", "71234567").resultado()).isEqualTo("ERROR");
		SERVIDOR.verify(1, anyRequestedFor(anyUrl()));
	}

	@Test
	void unTipoSinConsultaNoLlamaAlProveedor() {
		assertThat(consulta.consultar("4", "ABC123").resultado()).isEqualTo("ERROR");
		SERVIDOR.verify(0, anyRequestedFor(anyUrl()));
	}

	// ------------------------------------------------------------------ facturación

	private DocumentoElectronico boleta() {
		return new DocumentoElectronico("BOLETA", "B001", "00000123", LocalDate.of(2026, 11, 3), null, null, null, new BigDecimal("35.00"), BigDecimal.ZERO,
				List.of(new DocumentoElectronico.Linea("Corte", 1, new BigDecimal("35.00"))));
	}

	private String cdrZip() throws Exception {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(salida)) {
			zip.putNextEntry(new ZipEntry("R-20601234567-03-B001-123.xml"));
			zip.write("<cdr/>".getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		return Base64.getEncoder().encodeToString(salida.toByteArray());
	}

	private String respuestaSunat(String code, String notas) throws Exception {
		return "{\"xml\":\"<xml/>\",\"hash\":\"HASH1\",\"sunatResponse\":{\"success\":true,\"cdrZip\":\"" + cdrZip() + "\",\"cdrResponse\":{\"accepted\":true,\"code\":\"" + code
				+ "\",\"description\":\"La Boleta fue aceptada\",\"notes\":" + notas + "}}}";
	}

	@Test
	void emiteLaBoletaExoneradaConIgvCeroYDevuelveEstadoHashXmlYCdr() throws Exception {
		SERVIDOR.stubFor(post(urlPathEqualTo("/invoice/send")).willReturn(okJson(respuestaSunat("0", "[]"))));

		RespuestaCpe respuesta = facturacion.emitir(boleta());

		assertThat(respuesta).isEqualTo(new RespuestaCpe(EstadoSunat.ACEPTADO, "0", "La Boleta fue aceptada", "HASH1", "<xml/>", "<cdr/>"));
		SERVIDOR.verify(postRequestedFor(urlPathEqualTo("/invoice/send")).withHeader("Authorization", equalTo("Bearer token-facturacion"))
			.withRequestBody(matchingJsonPath("$.tipoDoc", equalTo("03")))
			.withRequestBody(matchingJsonPath("$.serie", equalTo("B001")))
			.withRequestBody(matchingJsonPath("$.correlativo", equalTo("00000123")))
			.withRequestBody(matchingJsonPath("$.company.ruc", equalTo("20601234567")))
			.withRequestBody(matchingJsonPath("$.client.tipoDoc", equalTo("0")))
			.withRequestBody(matchingJsonPath("$.details[0].tipAfeIgv", equalTo("20")))
			.withRequestBody(matchingJsonPath("$.mtoOperExoneradas", equalToJson("35.00")))
			.withRequestBody(matchingJsonPath("$.mtoIGV", equalToJson("0.00"))));
	}

	@Test
	void laFacturaLlevaElCodigoSunatDelComprador() throws Exception {
		SERVIDOR.stubFor(post(urlPathEqualTo("/invoice/send")).willReturn(okJson(respuestaSunat("0", "[]"))));

		facturacion.emitir(new DocumentoElectronico("FACTURA", "F001", "00000001", LocalDate.of(2026, 11, 3), "6", "20601234567", "EMPRESA DEMO S.A.C.",
				new BigDecimal("35.00"), BigDecimal.ZERO, List.of(new DocumentoElectronico.Linea("Corte", 1, new BigDecimal("35.00")))));

		SERVIDOR.verify(postRequestedFor(urlPathEqualTo("/invoice/send")).withRequestBody(matchingJsonPath("$.tipoDoc", equalTo("01")))
			.withRequestBody(matchingJsonPath("$.client.tipoDoc", equalTo("6")))
			.withRequestBody(matchingJsonPath("$.client.numDoc", equalTo("20601234567"))));
	}

	@Test
	void conObservacionesQuedaObservadoYConErrorDeSunatRechazado() throws Exception {
		SERVIDOR.stubFor(post(urlPathEqualTo("/invoice/send")).willReturn(okJson(respuestaSunat("0", "[\"4287 - Observación\"]"))));
		assertThat(facturacion.emitir(boleta()).estadoSunat()).isEqualTo(EstadoSunat.OBSERVADO);

		SERVIDOR.stubFor(post(urlPathEqualTo("/invoice/send"))
			.willReturn(okJson("{\"xml\":\"<xml/>\",\"hash\":\"H\",\"sunatResponse\":{\"success\":false,\"error\":{\"code\":\"2017\",\"message\":\"RUC inválido\"}}}")));
		RespuestaCpe rechazado = facturacion.emitir(boleta());
		assertThat(rechazado.estadoSunat()).isEqualTo(EstadoSunat.RECHAZADO);
		assertThat(rechazado.codigoRespuesta()).isEqualTo("2017");
	}

	@Test
	void siNoHayRespuestaDefinitivaQuedaPendiente() {
		SERVIDOR.stubFor(post(urlPathEqualTo("/invoice/send"))
			.willReturn(okJson("{\"sunatResponse\":{\"success\":false,\"error\":{\"code\":\"0109\",\"message\":\"SUNAT caído\"}}}")));
		assertThat(facturacion.emitir(boleta()).estadoSunat()).isEqualTo(EstadoSunat.PENDIENTE);

		SERVIDOR.stubFor(post(urlPathEqualTo("/invoice/send")).willReturn(serverError()));
		assertThat(facturacion.emitir(boleta()).estadoSunat()).isEqualTo(EstadoSunat.PENDIENTE);

		SERVIDOR.stubFor(post(urlPathEqualTo("/invoice/send")).willReturn(okJson("{}").withFixedDelay(DEMORA_MS)));
		assertThat(facturacion.emitir(boleta()).estadoSunat()).isEqualTo(EstadoSunat.PENDIENTE);
	}

	// ------------------------------------------------------------------ [It. 2 · CUS23] nota de crédito

	private DocumentoElectronico notaCredito() {
		return new DocumentoElectronico("NC_BOLETA", "BC01", "00000004", LocalDate.of(2026, 11, 9), null, null, null, new BigDecimal("35.00"), BigDecimal.ZERO,
				List.of(new DocumentoElectronico.Linea("Corte", 1, new BigDecimal("35.00"))),
				new DocumentoElectronico.NotaAfectada("BOLETA", "B001", "00000123", "06", "Devolución total"));
	}

	@Test
	void laNotaDeCreditoVaANoteSendConElComprobanteQueAnulaYSuMotivo() throws Exception {
		SERVIDOR.stubFor(post(urlPathEqualTo("/note/send")).willReturn(okJson(respuestaSunat("0", "[]"))));

		RespuestaCpe respuesta = facturacion.emitir(notaCredito());

		assertThat(respuesta.estadoSunat()).isEqualTo(EstadoSunat.ACEPTADO);
		assertThat(respuesta.hashCpe()).isEqualTo("HASH1");
		SERVIDOR.verify(postRequestedFor(urlPathEqualTo("/note/send")).withHeader("Authorization", equalTo("Bearer token-facturacion"))
			.withRequestBody(matchingJsonPath("$.tipoDoc", equalTo("07")))
			.withRequestBody(matchingJsonPath("$.serie", equalTo("BC01")))
			.withRequestBody(matchingJsonPath("$.correlativo", equalTo("00000004")))
			.withRequestBody(matchingJsonPath("$.tipDocAfectado", equalTo("03")))
			.withRequestBody(matchingJsonPath("$.numDocfectado", equalTo("B001-00000123")))
			.withRequestBody(matchingJsonPath("$.codMotivo", equalTo("06")))
			.withRequestBody(matchingJsonPath("$.desMotivo", equalTo("Devolución total")))
			.withRequestBody(matchingJsonPath("$.client.tipoDoc", equalTo("0")))
			.withRequestBody(matchingJsonPath("$.mtoImpVenta", equalToJson("35.00"))));
		SERVIDOR.verify(0, postRequestedFor(urlPathEqualTo("/invoice/send")));
	}

	@Test
	void laNotaDeCreditoQuedaPendienteSiElProveedorNoResponde() {
		SERVIDOR.stubFor(post(urlPathEqualTo("/note/send")).willReturn(serverError()));
		assertThat(facturacion.emitir(notaCredito()).estadoSunat()).isEqualTo(EstadoSunat.PENDIENTE);

		SERVIDOR.stubFor(post(urlPathEqualTo("/note/send")).willReturn(okJson("{}").withFixedDelay(DEMORA_MS)));
		assertThat(facturacion.emitir(notaCredito()).estadoSunat()).isEqualTo(EstadoSunat.PENDIENTE);
	}

	// ------------------------------------------------------------------ Izipay

	private SolicitudCobro solicitud() {
		return new SolicitudCobro(null, 7L, 4L, new BigDecimal("50.00"), "LINK", LocalDateTime.of(2026, 11, 4, 10, 0));
	}

	@Test
	void elReembolsoDeTarjetaSeApruebaConElUuidDeIzipayYSinRespuestaQuedaPendiente() {
		String ruta = "/api-payment/V4/Transaction/CancelOrRefund";
		SERVIDOR.stubFor(post(urlPathEqualTo(ruta)).willReturn(okJson("{\"status\":\"SUCCESS\",\"answer\":{\"uuid\":\"REEMB-1\"}}")));

		RespuestaPasarela aprobada = izipay.devolver(new SolicitudDevolucion("CS-ORDEN-1", 4L, new BigDecimal("50.00")));

		assertThat(aprobada.estado()).isEqualTo("APROBADA");
		assertThat(aprobada.codigoOperacion()).isEqualTo("REEMB-1");
		SERVIDOR.verify(postRequestedFor(urlPathEqualTo(ruta)).withRequestBody(matchingJsonPath("$.uuid", equalTo("CS-ORDEN-1")))
			.withRequestBody(matchingJsonPath("$.amount", equalToJson("5000"))));

		SERVIDOR.stubFor(post(urlPathEqualTo(ruta)).willReturn(okJson("{\"status\":\"ERROR\",\"answer\":{\"errorMessage\":\"Ya reembolsada\"}}")));
		assertThat(izipay.devolver(new SolicitudDevolucion("CS-ORDEN-1", 4L, new BigDecimal("50.00"))).estado()).isEqualTo("RECHAZADA");

		SERVIDOR.stubFor(post(urlPathEqualTo(ruta)).willReturn(serverError()));
		assertThat(izipay.devolver(new SolicitudDevolucion("CS-ORDEN-1", 4L, new BigDecimal("50.00"))).estado()).isEqualTo("PENDIENTE");
	}

	@Test
	void creaLaOrdenConElMontoEnCentavosYSinDatosDeTarjeta() {
		SERVIDOR.stubFor(post(urlPathEqualTo(RUTA_ORDEN))
			.willReturn(okJson("{\"status\":\"SUCCESS\",\"answer\":{\"paymentOrderId\":\"abc\",\"paymentURL\":\"https://pago/abc\"}}")));

		RespuestaPasarela respuesta = izipay.crearEnlace(solicitud());

		assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
		assertThat(respuesta.enlacePago()).isEqualTo("https://pago/abc");
		assertThat(respuesta.codigoOperacion()).startsWith("CS-").hasSizeLessThanOrEqualTo(60);
		SERVIDOR.verify(postRequestedFor(urlPathEqualTo(RUTA_ORDEN))
			.withHeader("Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString("usuario:clave".getBytes(StandardCharsets.UTF_8))))
			.withRequestBody(matchingJsonPath("$.amount", equalToJson("5000")))
			.withRequestBody(matchingJsonPath("$.currency", equalTo("PEN")))
			.withRequestBody(matchingJsonPath("$.orderId", equalTo(respuesta.codigoOperacion())))
			.withRequestBody(matchingJsonPath("$.expirationDate", equalTo("2026-11-04T10:00-05:00")))
			.withRequestBody(notContaining("pan"))
			.withRequestBody(notContaining("cvv")));
	}

	@Test
	void unErrorDeNegocioEsRechazadaYSinRespuestaQuedaPendienteSinEnlace() {
		SERVIDOR.stubFor(post(urlPathEqualTo(RUTA_ORDEN)).willReturn(okJson("{\"status\":\"ERROR\",\"answer\":{\"errorCode\":\"INT_009\",\"errorMessage\":\"Monto inválido\"}}")));
		RespuestaPasarela rechazada = izipay.crearCobro(solicitud());
		assertThat(rechazada.estado()).isEqualTo("RECHAZADA");
		assertThat(rechazada.mensaje()).isEqualTo("Monto inválido");

		SERVIDOR.stubFor(post(urlPathEqualTo(RUTA_ORDEN)).willReturn(serverError()));
		assertThat(izipay.crearCobro(solicitud()).estado()).isEqualTo("PENDIENTE");
		assertThat(izipay.crearCobro(solicitud()).enlacePago()).isNull();

		SERVIDOR.stubFor(post(urlPathEqualTo(RUTA_ORDEN)).willReturn(okJson("{}").withFixedDelay(DEMORA_MS)));
		assertThat(izipay.crearEnlace(solicitud()).enlacePago()).isNull();
	}

	private String firma(String respuesta) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(CLAVE_HMAC.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return HexFormat.of().formatHex(mac.doFinal(respuesta.getBytes(StandardCharsets.UTF_8)));
	}

	private String formulario(String respuesta, String firma) {
		return "kr-answer=" + URLEncoder.encode(respuesta, StandardCharsets.UTF_8) + "&kr-hash=" + firma + "&kr-hash-algorithm=sha256_hmac";
	}

	private String ipn(String estado, String ciclo) {
		return "{\"orderCycle\":\"" + ciclo + "\",\"orderStatus\":\"" + estado + "\",\"orderDetails\":{\"orderId\":\"CS-1\"}}";
	}

	@Test
	void traduceLaNotificacionConFirmaValida() throws Exception {
		String pagada = ipn("PAID", "CLOSED");
		assertThat(izipay.traducirNotificacion(formulario(pagada, firma(pagada)), null)).isEqualTo(new NotificacionPasarela("CS-1", "APROBADA"));

		String abierta = ipn("UNPAID", "OPEN");
		assertThat(izipay.traducirNotificacion(formulario(abierta, firma(abierta)), null).estado()).isEqualTo("PENDIENTE");

		String cerrada = ipn("UNPAID", "CLOSED");
		assertThat(izipay.traducirNotificacion(formulario(cerrada, firma(cerrada)), null).estado()).isEqualTo("RECHAZADA");
	}

	@Test
	void rechazaLaFirmaInvalidaYElCuerpoIlegible() throws Exception {
		String pagada = ipn("PAID", "CLOSED");
		assertThatThrownBy(() -> izipay.traducirNotificacion(formulario(pagada, "00ff"), null)).isInstanceOfSatisfying(ReglaNegocioException.class,
				ex -> assertThat(ex.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED));
		assertThatThrownBy(() -> izipay.traducirNotificacion("sin-firma=1", null)).isInstanceOfSatisfying(ReglaNegocioException.class,
				ex -> assertThat(ex.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED));

		String ilegible = "no es json";
		assertThatThrownBy(() -> izipay.traducirNotificacion(formulario(ilegible, firma(ilegible)), null)).isInstanceOfSatisfying(ReglaNegocioException.class,
				ex -> assertThat(ex.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST));
	}

	@Test
	void sinClaveConfiguradaNingunaFirmaEsValida() {
		AdaptadorIzipay sinClave = new AdaptadorIzipay(SERVIDOR.baseUrl(), "usuario", "clave", "", LIMITE);

		assertThatThrownBy(() -> sinClave.traducirNotificacion(formulario("{}", "00"), null)).isInstanceOf(ReglaNegocioException.class);
	}

}
