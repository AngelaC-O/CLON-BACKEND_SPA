package dev.com.Synapsis_Spa_Cautiva;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.jayway.jsonpath.JsonPath;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.GeneradorPdfComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.ServicioFacturacionElectronica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/** Los adaptadores reales de punta a punta, como en sandbox: Oracle en Testcontainers y los proveedores en WireMock. */
@SpringBootTest(properties = { "cautiva.integraciones.apisperu.modo=real", "cautiva.integraciones.izipay.modo=real", "cautiva.empresa.ruc=20601234567",
		"cautiva.empresa.razon-social=CAUTIVA SPA E.I.R.L.", "cautiva.empresa.direccion=Pichanaki", "cautiva.empresa.ubigeo=120604", "cautiva.empresa.departamento=JUNIN",
		"cautiva.empresa.provincia=CHANCHAMAYO", "cautiva.empresa.distrito=PICHANAKI" })
@AutoConfigureMockMvc
class IntegracionesRealesFlujoTest extends VentasFlujoApoyo {

	private static final String CLAVE_HMAC = "clave-hmac-solo-de-pruebas";

	private static final String CLAVE_ENLACE = "clave-enlace-solo-de-pruebas";

	private static final String RUTA_ORDEN = "/api-payment/V4/Charge/CreatePaymentOrder";

	private static final String RUTA_ENVIO = "/invoice/send";

	private static final WireMockServer SERVIDOR = new WireMockServer(WireMockConfiguration.options().dynamicPort());

	static {
		SERVIDOR.start();
	}

	@Autowired
	private ServicioFacturacionElectronica servicioFacturacionElectronica;

	@Autowired
	private GeneradorPdfComprobante generadorPdfComprobante;

	@DynamicPropertySource
	static void proveedores(DynamicPropertyRegistry registry) {
		registry.add("cautiva.integraciones.apisperu.consulta-url", SERVIDOR::baseUrl);
		registry.add("cautiva.integraciones.apisperu.token", () -> "token-consulta");
		registry.add("cautiva.integraciones.apisperu.facturacion-url", SERVIDOR::baseUrl);
		registry.add("cautiva.integraciones.apisperu.facturacion-token", () -> "token-facturacion");
		registry.add("cautiva.integraciones.izipay.api-url", SERVIDOR::baseUrl);
		registry.add("cautiva.integraciones.izipay.usuario", () -> "usuario");
		registry.add("cautiva.integraciones.izipay.clave", () -> "clave");
	}

	@BeforeEach
	void limpiar() {
		SERVIDOR.resetAll();
	}

	private String hmac(String clave, String datos) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return HexFormat.of().formatHex(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
	}

	private ResultActions notificar(String respuesta, String firma) throws Exception {
		return mvc.perform(post("/api/v1/pagos/notificaciones").contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.content("kr-answer=" + URLEncoder.encode(respuesta, StandardCharsets.UTF_8) + "&kr-hash=" + firma + "&kr-hash-algorithm=sha256_hmac"));
	}

	private String ipnPagada(String codigo) {
		return "{\"orderCycle\":\"CLOSED\",\"orderStatus\":\"PAID\",\"orderDetails\":{\"orderId\":\"" + codigo + "\"}}";
	}

	private void ordenCreada() {
		SERVIDOR.stubFor(WireMock.post(urlPathEqualTo(RUTA_ORDEN))
			.willReturn(okJson("{\"status\":\"SUCCESS\",\"answer\":{\"paymentURL\":\"https://pago.izipay.invalid/" + UUID.randomUUID() + "\"}}")));
	}

	private void sunatAcepta() throws Exception {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(salida)) {
			zip.putNextEntry(new ZipEntry("R.xml"));
			zip.write("<cdr/>".getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		SERVIDOR.stubFor(WireMock.post(urlPathEqualTo(RUTA_ENVIO))
			.willReturn(okJson("{\"xml\":\"<xml>firmado</xml>\",\"hash\":\"HASHREAL123\",\"sunatResponse\":{\"success\":true,\"cdrZip\":\""
					+ Base64.getEncoder().encodeToString(salida.toByteArray()) + "\",\"cdrResponse\":{\"code\":\"0\",\"description\":\"Aceptada\",\"notes\":[]}}}")));
	}

	private Long citaNueva(int semana) throws Exception {
		Long idCliente = clienta("Integracion " + semana);
		return idDe(enviar(post("/api/v1/citas"), "{\"idCliente\":" + idCliente + ",\"idPersonal\":" + especialista() + ",\"idRecurso\":" + recurso(0) + ",\"idVariante\":"
				+ variante(CORTE) + ",\"fechaHoraInicio\":\"" + lunes(semana).atTime(10, 0) + "\",\"duracionMin\":30}").andExpect(status().isCreated()), "idCita");
	}

	private String texto(byte[] pdf) throws Exception {
		try (PdfReader lector = new PdfReader(pdf)) {
			return new PdfTextExtractor(lector).getTextFromPage(1);
		}
	}

	// ------------------------------------------------------------------ AdaptadorApisPeruConsulta

	@Test
	void laConsultaEncontradaCompletaLaFichaYQuedaVerificada() throws Exception {
		String dni = "76543210";
		Long idDni = id("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'");
		SERVIDOR.stubFor(WireMock.get(urlPathEqualTo("/dni/" + dni)).willReturn(okJson("{\"nombres\":\"ANA\",\"apellidoPaterno\":\"QUISPE\",\"apellidoMaterno\":\"ROJAS\"}")));

		String respuesta = enviar(get("/api/v1/clientes/consulta-documento").param("idTipoDocumento", idDni.toString()).param("numeroDocumento", dni), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.resultado").value("ENCONTRADO"))
			.andExpect(jsonPath("$.apellidos").value("QUISPE ROJAS"))
			.andReturn().getResponse().getContentAsString();
		String fechaVerificacion = JsonPath.read(respuesta, "$.fechaVerificacion");

		enviar(post("/api/v1/clientes"), "{\"idTipoDocumento\":" + idDni + ",\"numeroDocumento\":\"" + dni + "\",\"nombres\":\"ANA\",\"apellidos\":\"QUISPE ROJAS\",\"telefono\":\"987229901\","
				+ "\"aceptaComunicaciones\":true,\"fechaVerificacion\":\"" + fechaVerificacion + "\"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.origenDatos").value("VERIFICADO"));
	}

	@Test
	void siLaConsultaFallaLaRespuestaEs202YSeSigueAMano() throws Exception {
		Long idDni = id("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'");
		SERVIDOR.stubFor(WireMock.get(urlPathEqualTo("/dni/76543211")).willReturn(serverError()));

		enviar(get("/api/v1/clientes/consulta-documento").param("idTipoDocumento", idDni.toString()).param("numeroDocumento", "76543211"), null)
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.resultado").value("ERROR"))
			.andExpect(jsonPath("$.fechaVerificacion").doesNotExist());
	}

	// ------------------------------------------------------------------ AdaptadorApisPeruFacturacion y ReenvioCpeJob

	@Test
	void elCierreEmiteYGuardaLaRespuestaDeSunatYElPdfLlevaElHash() throws Exception {
		sunatAcepta();

		Long idComprobante = idDe(ventaCerrada().andExpect(jsonPath("$.comprobante.estadoSunat").value("ACEPTADO")), "comprobante.idComprobante");

		assertThat(jdbc.queryForObject("SELECT hash_cpe FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idComprobante)).isEqualTo("HASHREAL123");
		assertThat(jdbc.queryForObject("SELECT ruta_cdr FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idComprobante)).endsWith("-cdr.xml");

		byte[] pdf = enviar(get("/api/v1/comprobantes/" + idComprobante + "/representacion"), null).andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF))
			.andReturn().getResponse().getContentAsByteArray();
		assertThat(texto(pdf)).contains("HASHREAL123", "20601234567", "ACEPTADO");
	}

	/** [It. 2.1 · D-33] La línea de un producto del CPE que va a APISPERU lleva el nombre del producto (PRODUCTO), no «PRODUCTO». */
	@Test
	void elCpeDeUnProductoLlevaSuNombre() throws Exception {
		sunatAcepta();
		cajaAbierta();
		Long idCategoria = id("SELECT MIN(id_categoria) FROM CATEGORIA_PRODUCTO WHERE nombre <> 'Insumos de servicio'");
		String nombre = "Crema del CPE " + sedeDeLaClase();
		Long idProducto = idDe(enviarEnSede("almacen", sedeDeLaClase(), post("/api/v1/productos"), "{\"codigo\":\"CPE-" + sedeDeLaClase() + "\",\"nombre\":\""
				+ nombre + "\",\"idCategoria\":" + idCategoria + ",\"unidad\":\"UND\",\"tipoUso\":\"VENTA\",\"costo\":10.00,\"stockMinimo\":0}")
			.andExpect(status().isCreated()), "idProducto");
		enviarEnSede("almacen", sedeDeLaClase(), post("/api/v1/productos/" + idProducto + "/ajustes"), "{\"cantidad\":3,\"motivo\":\"Conteo inicial\"}")
			.andExpect(status().isOk());
		java.math.BigDecimal precio = jdbc.queryForObject("SELECT precio_venta FROM PRODUCTO WHERE id_producto = ?", java.math.BigDecimal.class, idProducto);

		enviar(post("/api/v1/ventas/productos"), "{\"lineas\":[{\"idProducto\":" + idProducto + ",\"cantidad\":1}],\"idMedioPago\":" + medio("EFECTIVO")
				+ ",\"monto\":" + precio.toPlainString() + ",\"tipo\":\"BOLETA\"}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.comprobante.estadoSunat").value("ACEPTADO"));

		SERVIDOR.verify(WireMock.postRequestedFor(urlPathEqualTo(RUTA_ENVIO))
			.withRequestBody(WireMock.matchingJsonPath("$.details[0].descripcion", WireMock.equalTo(nombre))));
	}

	@Test
	void siElProveedorNoRespondeLaVentaSeCierraYElJobLaReenvia() throws Exception {
		SERVIDOR.stubFor(WireMock.post(urlPathEqualTo(RUTA_ENVIO)).willReturn(serverError()));
		Long idComprobante = idDe(ventaCerrada().andExpect(jsonPath("$.estado").value("CERRADA")).andExpect(jsonPath("$.comprobante.estadoSunat").value("PENDIENTE")),
				"comprobante.idComprobante");

		sunatAcepta();
		assertThat(servicioFacturacionElectronica.reenviarPendientes()).isPositive();

		assertThat(jdbc.queryForObject("SELECT estado_sunat FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idComprobante)).isEqualTo("ACEPTADO");
		assertThat(id("SELECT intentos_envio FROM COMPROBANTE WHERE id_comprobante = ?", idComprobante)).isEqualTo(2);
	}

	// ------------------------------------------------------------------ representación impresa

	@Test
	void elQrSeLeeConElTextoDelComprobante() throws Exception {
		String texto = "20601234567|03|B001|123|35.00|2026-11-03||HASH";

		var imagen = generadorPdfComprobante.generarQr(texto);

		assertThat(new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)))).getText()).isEqualTo(texto);
	}

	@Test
	void elEnlacePublicoAbreSinSesionYRechazaUnaFirmaMalaOVencida() throws Exception {
		sunatAcepta();
		Long idComprobante = idDe(ventaCerrada(), "comprobante.idComprobante");

		String enlace = JsonPath.read(enviar(get("/api/v1/comprobantes/" + idComprobante + "/enlace"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.enlace", startsWith("http://localhost/api/v1/comprobantes/publico/" + idComprobante)))
			.andReturn().getResponse().getContentAsString(), "$.enlace");

		mvc.perform(get(enlace)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF));

		mvc.perform(get(enlace.replace("/publico/" + idComprobante, "/publico/" + (idComprobante + 1)))).andExpect(status().isForbidden());
		mvc.perform(get(enlace.replaceAll("firma=.*", "firma=00ff"))).andExpect(status().isForbidden());

		long vencido = Instant.now().minusSeconds(60).getEpochSecond();
		String firmaVencida = hmac(CLAVE_ENLACE, idComprobante + ":" + vencido);
		mvc.perform(get("/api/v1/comprobantes/publico/" + idComprobante).param("expira", String.valueOf(vencido)).param("firma", firmaVencida)).andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ AdaptadorIzipay y webhook

	@Test
	void elWebhookConFirmaValidaRegistraElAdelantoUnaSolaVezYConFirmaInvalidaNo() throws Exception {
		Long idCaja = cajaAbierta();
		ordenCreada();
		Long idCita = citaNueva(30);
		Long idTransaccion = idDe(enviar(post("/api/v1/citas/" + idCita + "/enlace-adelanto"), "{\"adelantoMonto\":50,\"idMedioPago\":" + medio("TARJETA") + "}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.enlacePago", startsWith("https://pago.izipay.invalid/"))), "idTransaccion");
		String codigo = jdbc.queryForObject("SELECT codigo_operacion FROM TRANSACCION_PASARELA WHERE id_transaccion = ?", String.class, idTransaccion);
		String respuesta = ipnPagada(codigo);

		notificar(respuesta, "firma-invalida").andExpect(status().isUnauthorized());
		assertThat(id("SELECT adelanto_monto FROM CITA WHERE id_cita = ?", idCita)).isZero();

		notificar(respuesta, hmac(CLAVE_HMAC, respuesta)).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("SELECT estado FROM TRANSACCION_PASARELA WHERE id_transaccion = ?", String.class, idTransaccion)).isEqualTo("APROBADA");
		assertThat(id("SELECT adelanto_monto FROM CITA WHERE id_cita = ?", idCita)).isEqualTo(50);
		assertThat(id("SELECT id_caja FROM CITA WHERE id_cita = ?", idCita)).isEqualTo(idCaja);

		notificar(respuesta, hmac(CLAVE_HMAC, respuesta)).andExpect(status().isOk());
		assertThat(id("SELECT COUNT(*) FROM TRANSACCION_PASARELA WHERE id_cita = ?", idCita)).isEqualTo(1);
	}

	@Test
	void siIzipayNoRespondeElEnlaceEs503YNoQuedaTransaccion() throws Exception {
		SERVIDOR.stubFor(WireMock.post(urlPathEqualTo(RUTA_ORDEN)).willReturn(serverError()));
		Long idCita = citaNueva(31);

		enviar(post("/api/v1/citas/" + idCita + "/enlace-adelanto"), "{\"adelantoMonto\":50,\"idMedioPago\":" + medio("TARJETA") + "}").andExpect(status().isServiceUnavailable());

		assertThat(id("SELECT COUNT(*) FROM TRANSACCION_PASARELA WHERE id_cita = ?", idCita)).isZero();
		SERVIDOR.verify(1, anyRequestedFor(anyUrl()));
	}

	@Test
	void elCobroQrQuedaPendienteHastaElWebhookYLuegoCierraLaVenta() throws Exception {
		cajaAbierta();
		ordenCreada();
		sunatAcepta();
		Long idVenta = idDe(crearVenta(atencionFinalizada(null, variante(CORTE))).andExpect(status().isCreated()), "idVenta");

		ResultActions pendiente = pagar(idVenta, medio("YAPE"), "35", "QR").andExpect(status().isAccepted())
			.andExpect(jsonPath("$.pagos", hasSize(0)))
			.andExpect(jsonPath("$.cobrosPendientes[0].enlacePago", startsWith("https://pago.izipay.invalid/")));
		Long idTransaccion = idDe(pendiente, "cobrosPendientes[0].idTransaccion");
		String cuerpo = "{\"pagos\":[{\"idMedioPago\":" + medio("YAPE") + ",\"monto\":35,\"idTransaccion\":" + idTransaccion + "}]}";

		// RN42: sin aprobar no respalda el pago
		enviar(post("/api/v1/ventas/" + idVenta + "/pagos"), cuerpo).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RN42"));
		cerrarConBoleta(idVenta).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RN11"));

		String codigo = jdbc.queryForObject("SELECT codigo_operacion FROM TRANSACCION_PASARELA WHERE id_transaccion = ?", String.class, idTransaccion);
		String respuesta = ipnPagada(codigo);
		notificar(respuesta, hmac(CLAVE_HMAC, respuesta)).andExpect(status().isOk());

		enviar(post("/api/v1/ventas/" + idVenta + "/pagos"), cuerpo).andExpect(status().isOk()).andExpect(jsonPath("$.pendiente").value(0.0));
		cerrarConBoleta(idVenta).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADA")).andExpect(jsonPath("$.comprobante.estadoSunat").value("ACEPTADO"));
	}

}
