package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Boleta o factura por APISPERU (contrato sin verificar con el proveedor: DF-49). Con IGV 0 la operación es exonerada (RN06). Si no responde o la
 * respuesta no es definitiva, queda PENDIENTE y ReenvioCpeJob la reintenta (RN46).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "cautiva.integraciones.apisperu.modo", havingValue = "real")
public class AdaptadorApisPeruFacturacion implements FacturacionElectronicaPort {

	private static final String RUTA_ENVIO = "/invoice/send";

	private static final String RUTA_NOTA = "/note/send";

	private static final String CODIGO_NOTA_CREDITO = "07";

	private static final String BOLETA = "BOLETA";

	private static final String CODIGO_BOLETA = "03";

	private static final String CODIGO_FACTURA = "01";

	private static final String AFECTACION_EXONERADA = "20";

	private static final String AFECTACION_GRAVADA = "10";

	/** Sin documento: la boleta menor al umbral no lo exige (RN08). */
	private static final String DOC_SIN_IDENTIFICAR = "0";

	private static final String NUM_SIN_IDENTIFICAR = "00000000";

	private static final String NOMBRE_SIN_IDENTIFICAR = "CLIENTE VARIOS";

	private static final String CODIGO_ACEPTADO = "0";

	/** Errores de SUNAT que rechazan el comprobante; los demás códigos se reintentan. */
	private static final int RECHAZO_MIN = 2000;

	private static final int RECHAZO_MAX = 3999;

	private static final int MAX_DESCRIPCION = 500;

	private static final int MAX_CODIGO = 10;

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final RestClient restClient;

	private final Empresa empresa;

	public AdaptadorApisPeruFacturacion(@Value("${cautiva.integraciones.apisperu.facturacion-url}") String url,
			@Value("${cautiva.integraciones.apisperu.facturacion-token}") String token,
			@Value("${cautiva.integraciones.apisperu.facturacion-limite:10s}") Duration limite, @Value("${cautiva.empresa.ruc}") String ruc,
			@Value("${cautiva.empresa.razon-social}") String razonSocial, @Value("${cautiva.empresa.direccion}") String direccion,
			@Value("${cautiva.empresa.ubigeo}") String ubigeo, @Value("${cautiva.empresa.departamento}") String departamento,
			@Value("${cautiva.empresa.provincia}") String provincia, @Value("${cautiva.empresa.distrito}") String distrito) {
		HttpClient cliente = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(limite).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
		fabrica.setReadTimeout(limite);
		this.restClient = RestClient.builder()
			.baseUrl(url)
			.requestFactory(fabrica)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.build();
		this.empresa = new Empresa(ruc, razonSocial, new Direccion(ubigeo, departamento, provincia, distrito, direccion));
	}

	/** [It. 2 · CUS23] Una nota de crédito va a /note/send con el comprobante que anula; boletas y facturas, a /invoice/send. */
	@Override
	public RespuestaCpe emitir(DocumentoElectronico oDocumentoElectronico) {
		boolean esNota = oDocumentoElectronico.notaCredito() != null;
		try {
			RespuestaEnvio respuesta = restClient.post()
				.uri(esNota ? RUTA_NOTA : RUTA_ENVIO)
				.contentType(MediaType.APPLICATION_JSON)
				.body(esNota ? aNota(oDocumentoElectronico) : aFactura(oDocumentoElectronico))
				.retrieve()
				.body(RespuestaEnvio.class);
			return traducir(respuesta);
		}
		catch (RestClientException ex) {
			log.warn("APISPERU no respondió el comprobante {}-{}: {}", oDocumentoElectronico.serie(), oDocumentoElectronico.numero(), ex.getClass().getSimpleName());
			return pendiente("El proveedor no respondió");
		}
	}

	private RespuestaCpe traducir(RespuestaEnvio respuesta) {
		if (respuesta == null || respuesta.sunatResponse() == null) {
			return pendiente("Respuesta vacía del proveedor");
		}
		RespuestaSunat sunat = respuesta.sunatResponse();
		if (Boolean.TRUE.equals(sunat.success()) && sunat.cdrResponse() != null && sunat.cdrZip() != null) {
			RespuestaCdr cdr = sunat.cdrResponse();
			return new RespuestaCpe(estadoDe(cdr), limitar(cdr.code(), MAX_CODIGO), limitar(cdr.description(), MAX_DESCRIPCION), respuesta.hash(), respuesta.xml(),
					leerCdr(sunat.cdrZip()));
		}
		if (sunat.error() != null && esRechazo(sunat.error().code())) {
			return new RespuestaCpe(EstadoSunat.RECHAZADO, limitar(sunat.error().code(), MAX_CODIGO), limitar(sunat.error().message(), MAX_DESCRIPCION), respuesta.hash(),
					respuesta.xml(), null);
		}
		return pendiente(sunat.error() == null ? "SUNAT no dio una respuesta definitiva" : limitar(sunat.error().message(), MAX_DESCRIPCION));
	}

	private static EstadoSunat estadoDe(RespuestaCdr cdr) {
		if (!CODIGO_ACEPTADO.equals(cdr.code())) {
			return esRechazo(cdr.code()) ? EstadoSunat.RECHAZADO : EstadoSunat.OBSERVADO;
		}
		return cdr.notes() == null || cdr.notes().isEmpty() ? EstadoSunat.ACEPTADO : EstadoSunat.OBSERVADO;
	}

	private static boolean esRechazo(String codigo) {
		try {
			int numero = Integer.parseInt(codigo);
			return numero >= RECHAZO_MIN && numero <= RECHAZO_MAX;
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

	/** El CDR llega como un zip en base64 con un solo XML; AlmacenArchivosPort guarda texto. */
	private static String leerCdr(String cdrZip) {
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(cdrZip)))) {
			ZipEntry entrada = zip.getNextEntry();
			return entrada == null ? cdrZip : new String(zip.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException | IllegalArgumentException ex) {
			return cdrZip;
		}
	}

	private Factura aFactura(DocumentoElectronico oDocumento) {
		BigDecimal porcentaje = oDocumento.igvPorcentaje();
		boolean exonerada = porcentaje.signum() == 0;
		List<Detalle> detalles = oDocumento.lineas().stream().map(linea -> aDetalle(linea, porcentaje, exonerada)).toList();
		BigDecimal valorVenta = detalles.stream().map(Detalle::mtoValorVenta).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal igv = detalles.stream().map(Detalle::igv).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Factura("2.1", "0101", BOLETA.equals(oDocumento.tipo()) ? CODIGO_BOLETA : CODIGO_FACTURA, oDocumento.serie(), oDocumento.numero(),
				oDocumento.fechaEmision() + "T00:00:00-05:00", new FormaPago("PEN", "Contado"), "PEN", aCliente(oDocumento), empresa, exonerada ? null : valorVenta,
				exonerada ? valorVenta : null, igv, igv, valorVenta, oDocumento.importe(), oDocumento.importe(), detalles,
				List.of(new Leyenda("1000", "SON " + oDocumento.importe().setScale(2, RoundingMode.HALF_UP) + " SOLES")));
	}

	/** [It. 2 · CUS23] Nota de crédito (tipo 07) con los mismos montos y líneas que el comprobante que anula, su tipo, serie-número y motivo. */
	private Nota aNota(DocumentoElectronico oDocumento) {
		BigDecimal porcentaje = oDocumento.igvPorcentaje();
		boolean exonerada = porcentaje.signum() == 0;
		List<Detalle> detalles = oDocumento.lineas().stream().map(linea -> aDetalle(linea, porcentaje, exonerada)).toList();
		BigDecimal valorVenta = detalles.stream().map(Detalle::mtoValorVenta).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal igv = detalles.stream().map(Detalle::igv).reduce(BigDecimal.ZERO, BigDecimal::add);
		DocumentoElectronico.NotaAfectada afectada = oDocumento.notaCredito();
		return new Nota("2.1", CODIGO_NOTA_CREDITO, oDocumento.serie(), oDocumento.numero(), oDocumento.fechaEmision() + "T00:00:00-05:00",
				BOLETA.equals(afectada.tipoAfectado()) ? CODIGO_BOLETA : CODIGO_FACTURA, afectada.serieAfectada() + "-" + afectada.numeroAfectado(),
				afectada.codigoMotivo(), afectada.motivo(), "PEN", aCliente(oDocumento), empresa, exonerada ? null : valorVenta, exonerada ? valorVenta : null,
				igv, igv, oDocumento.importe(), detalles,
				List.of(new Leyenda("1000", "SON " + oDocumento.importe().setScale(2, RoundingMode.HALF_UP) + " SOLES")));
	}

	private static Cliente aCliente(DocumentoElectronico oDocumento) {
		if (oDocumento.nroDocComprador() == null) {
			return new Cliente(DOC_SIN_IDENTIFICAR, NUM_SIN_IDENTIFICAR, oDocumento.nombreComprador() == null ? NOMBRE_SIN_IDENTIFICAR : oDocumento.nombreComprador());
		}
		return new Cliente(oDocumento.codigoSunatComprador(), oDocumento.nroDocComprador(), oDocumento.nombreComprador());
	}

	private static Detalle aDetalle(DocumentoElectronico.Linea linea, BigDecimal porcentaje, boolean exonerada) {
		BigDecimal cantidad = BigDecimal.valueOf(linea.cantidad());
		BigDecimal valorVenta = exonerada ? linea.subtotal()
				: linea.subtotal().multiply(CIEN).divide(CIEN.add(porcentaje), 2, RoundingMode.HALF_UP);
		BigDecimal igv = linea.subtotal().subtract(valorVenta);
		return new Detalle("NIU", linea.descripcion(), linea.cantidad(), valorVenta.divide(cantidad, 2, RoundingMode.HALF_UP), valorVenta, valorVenta, porcentaje, igv,
				exonerada ? AFECTACION_EXONERADA : AFECTACION_GRAVADA, igv, linea.subtotal().divide(cantidad, 2, RoundingMode.HALF_UP));
	}

	private static RespuestaCpe pendiente(String descripcion) {
		return new RespuestaCpe(EstadoSunat.PENDIENTE, null, descripcion, null, null, null);
	}

	private static String limitar(String texto, int maximo) {
		return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record Factura(String ublVersion, String tipoOperacion, String tipoDoc, String serie, String correlativo, String fechaEmision, FormaPago formaPago,
			String tipoMoneda, Cliente client, Empresa company, BigDecimal mtoOperGravadas, BigDecimal mtoOperExoneradas, BigDecimal mtoIGV, BigDecimal totalImpuestos,
			BigDecimal valorVenta, BigDecimal subTotal, BigDecimal mtoImpVenta, List<Detalle> details, List<Leyenda> legends) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record Nota(String ublVersion, String tipoDoc, String serie, String correlativo, String fechaEmision, String tipDocAfectado, String numDocfectado,
			String codMotivo, String desMotivo, String tipoMoneda, Cliente client, Empresa company, BigDecimal mtoOperGravadas, BigDecimal mtoOperExoneradas,
			BigDecimal mtoIGV, BigDecimal totalImpuestos, BigDecimal mtoImpVenta, List<Detalle> details, List<Leyenda> legends) {
	}

	private record FormaPago(String moneda, String tipo) {
	}

	private record Cliente(String tipoDoc, String numDoc, String rznSocial) {
	}

	private record Empresa(String ruc, String razonSocial, Direccion address) {
	}

	private record Direccion(String ubigueo, String departamento, String provincia, String distrito, String direccion) {
	}

	private record Detalle(String unidad, String descripcion, Integer cantidad, BigDecimal mtoValorUnitario, BigDecimal mtoValorVenta, BigDecimal mtoBaseIgv,
			BigDecimal porcentajeIgv, BigDecimal igv, String tipAfeIgv, BigDecimal totalImpuestos, BigDecimal mtoPrecioUnitario) {
	}

	private record Leyenda(String code, String value) {
	}

	private record RespuestaEnvio(String xml, String hash, RespuestaSunat sunatResponse) {
	}

	private record RespuestaSunat(Boolean success, String cdrZip, RespuestaCdr cdrResponse, ErrorSunat error) {
	}

	private record RespuestaCdr(String code, String description, List<String> notes) {
	}

	private record ErrorSunat(String code, String message) {
	}

}
