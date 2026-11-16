package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import dev.com.Synapsis_Spa_Cautiva.pagos.service.PasarelaPagosPort;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.RespuestaPasarela;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.FacturacionElectronicaPort;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.RespuestaCpe;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * [It. 2] CUS25 y CUS26 (venta de productos), CUS22 (solicitud), CUS23 (bandeja con nota de crédito) y CUS24 (devoluciones), en su propia sede de
 * VentasFlujoApoyo con series de nota de crédito propias. Los adaptadores son los falsos (APISPERU deja PENDIENTE, Izipay aprueba), espiados: una
 * prueba puede hacer que SUNAT acepte el comprobante (SUNAT solo admite la nota de un comprobante aceptado, -20076) o que el cobro quede PENDIENTE.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnulacionVentasFlujoTest extends VentasFlujoApoyo {

	private static final AtomicInteger CODIGOS = new AtomicInteger(500);

	private static final AtomicInteger TELEFONOS = new AtomicInteger(987_660_000);

	private static boolean seriesNotaCredito;

	@MockitoSpyBean
	private FacturacionElectronicaPort facturacionElectronicaPort;

	@MockitoSpyBean
	private PasarelaPagosPort pasarelaPagosPort;

	// ------------------------------------------------------------------ apoyo

	private ResultActions como(String usuario, MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		return enviarEnSede(usuario, sedeDeLaClase(), pedido, cuerpo);
	}

	/** La sede de la clase solo trae boleta y factura (VentasFlujoApoyo): las series de nota de crédito se agregan una vez. */
	private void seriesDeNotaCredito() throws SQLException {
		if (!seriesNotaCredito) {
			String sufijo = String.format("%02d", sedeDeLaClase() % 100);
			comoDueno("INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (" + sedeDeLaClase() + ", 'NC_BOLETA', 'BN" + sufijo + "')");
			comoDueno("INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (" + sedeDeLaClase() + ", 'NC_FACTURA', 'FN" + sufijo + "')");
			seriesNotaCredito = true;
		}
	}

	private Long clientaNueva(String nombres) throws Exception {
		return idDe(enviar(post("/api/v1/clientes"), "{\"nombres\":\"" + nombres + "\",\"telefono\":\"" + TELEFONOS.incrementAndGet() + "\",\"aceptaComunicaciones\":true}")
			.andExpect(status().isCreated()), "idCliente");
	}

	/** CUS32 y CUS34 como almacén en la sede de la clase: un producto de VENTA con su stock inicial. */
	private Long productoConStock(String nombre, int stock) throws Exception {
		Long idCategoria = id("SELECT MIN(id_categoria) FROM CATEGORIA_PRODUCTO WHERE nombre <> 'Insumos de servicio'");
		String cuerpo = "{\"codigo\":\"ANU-" + CODIGOS.incrementAndGet() + "\",\"nombre\":\"" + nombre + "\",\"idCategoria\":" + idCategoria
				+ ",\"unidad\":\"UND\",\"tipoUso\":\"VENTA\",\"costo\":10.00,\"stockMinimo\":0,\"ubicacion\":\"Vitrina\"}";
		Long idProducto = idDe(como("almacen", post("/api/v1/productos"), cuerpo).andExpect(status().isCreated()), "idProducto");
		como("almacen", post("/api/v1/productos/" + idProducto + "/ajustes"), "{\"cantidad\":" + stock + ",\"motivo\":\"Conteo inicial\"}")
			.andExpect(status().isOk());
		return idProducto;
	}

	private BigDecimal precio(Long idProducto) {
		return jdbc.queryForObject("SELECT precio_venta FROM PRODUCTO WHERE id_producto = ?", BigDecimal.class, idProducto);
	}

	private Integer stock(Long idProducto) throws SQLException {
		return jdbc.queryForObject("SELECT stock_actual FROM INVENTARIO_SEDE WHERE id_producto = ? AND id_sede = ?", Integer.class, idProducto,
				sedeDeLaClase());
	}

	private ResultActions vender(Long idCliente, Long idProducto, int cantidad, String medioPago, String canal) throws Exception {
		String cliente = idCliente == null ? "" : "\"idCliente\":" + idCliente + ",";
		String conCanal = canal == null ? "" : ",\"canal\":\"" + canal + "\"";
		BigDecimal monto = precio(idProducto).multiply(BigDecimal.valueOf(cantidad));
		return enviar(post("/api/v1/ventas/productos"), "{" + cliente + "\"lineas\":[{\"idProducto\":" + idProducto + ",\"cantidad\":" + cantidad
				+ ",\"idPersonal\":" + especialista() + "}],\"idMedioPago\":" + medio(medioPago) + ",\"monto\":" + monto.toPlainString() + conCanal
				+ ",\"tipo\":\"BOLETA\"}");
	}

	/** Venta CERRADA cuya boleta SUNAT ya aceptó: solo entonces se emite su nota de crédito (-20076). */
	private Long ventaAceptada(Long idCliente, String medioPago, String canal) throws Exception {
		seriesDeNotaCredito();
		cajaAbierta();
		sunatAcepta();
		return idDe(vender(idCliente, productoConStock("Para anular", 5), 1, medioPago, canal).andExpect(status().isCreated()), "idVenta");
	}

	private void sunatAcepta() {
		// CK_COMPR_CDR: un comprobante ACEPTADO guarda su CDR
		doReturn(new RespuestaCpe(EstadoSunat.ACEPTADO, "0", "Aceptado por SUNAT", "HASH-PRUEBA", "<xml/>", "<cdr/>")).when(facturacionElectronicaPort)
			.emitir(any());
	}

	private ResultActions solicitar(Long idVenta, String forma) throws Exception {
		return enviar(post("/api/v1/ventas/" + idVenta + "/solicitudes-anulacion"),
				"{\"motivo\":\"La clienta desistió\",\"codigoMotivo\":\"06\",\"formaDevolucion\":\"" + forma + "\"}");
	}

	private ResultActions aprobar(String usuario, Long idSolicitud) throws Exception {
		return como(usuario, post("/api/v1/solicitudes-anulacion/" + idSolicitud + "/aprobacion"), null);
	}

	private String estadoVenta(Long idVenta) {
		return jdbc.queryForObject("SELECT estado FROM VENTA WHERE id_venta = ?", String.class, idVenta);
	}

	// ------------------------------------------------------------------ CUS25 y CUS26

	@Test
	void cus26VendeProductosEnEfectivoYOracleDescuentaElStock() throws Exception {
		cajaAbierta();
		Long idProducto = productoConStock("Shampoo de anulación", 10);
		Long idCliente = clientaNueva("Compradora");

		enviar(get("/api/v1/ventas/productos").param("nombre", "Shampoo de anulación"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].idProducto").value(idProducto))
			.andExpect(jsonPath("$[0].stockActual").value(10));

		Long idVenta = idDe(vender(idCliente, idProducto, 2, "EFECTIVO", null).andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("CERRADA"))
			.andExpect(jsonPath("$.detalles[0].tipoItem").value("PRODUCTO"))
			.andExpect(jsonPath("$.detalles[0].idProducto").value(idProducto))
			.andExpect(jsonPath("$.detalles[0].idPersonal").value(especialista()))
			.andExpect(jsonPath("$.comprobante.tipo").value("BOLETA")), "idVenta");

		// RF38, RN38: la especialista que recomendó queda en la línea, sin comisión; TRG_VENTA_EFECTOS registra la SALIDA (RN18)
		assertThat(id("SELECT NVL(monto_comision, 0) FROM VENTA_DETALLE WHERE id_venta = ?", idVenta)).isZero();
		assertThat(stock(idProducto)).isEqualTo(8);
		assertThat(id("SELECT COUNT(*) FROM MOVIMIENTO_KARDEX m JOIN VENTA_DETALLE d ON d.id_detalle = m.id_detalle_venta WHERE d.id_venta = ? AND m.tipo = 'SALIDA'",
				idVenta)).isEqualTo(1);
	}

	@Test
	void cus26ConPagoDigitalPendienteEs202YLaVentaQuedaAbierta() throws Exception {
		cajaAbierta();
		Long idProducto = productoConStock("Crema pendiente", 3);
		doReturn(new RespuestaPasarela("PENDIENTE", "PEND-PROD-" + idProducto, "https://pasarela-falsa.invalid/qr", "En proceso")).when(pasarelaPagosPort)
			.crearCobro(any());

		Long idVenta = idDe(vender(null, idProducto, 1, "YAPE", "QR").andExpect(status().isAccepted())
			.andExpect(jsonPath("$.estado").value("ABIERTA"))
			.andExpect(jsonPath("$.cobrosPendientes", hasSize(1)))
			.andExpect(jsonPath("$.pagos", hasSize(0))), "idVenta");

		assertThat(estadoVenta(idVenta)).isEqualTo("ABIERTA");
		assertThat(stock(idProducto)).isEqualTo(3);
		// [It. 2.1 · D-22] La venta se confirmó antes de cobrar: el cobro guarda id_venta y la venta lo lista al retomarla
		assertThat(id("SELECT id_venta FROM TRANSACCION_PASARELA WHERE codigo_operacion = ?", "PEND-PROD-" + idProducto)).isEqualTo(idVenta);
		enviar(get("/api/v1/ventas/" + idVenta), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.cobrosPendientes", hasSize(1)))
			.andExpect(jsonPath("$.cobrosPendientes[0].enlacePago").value("https://pasarela-falsa.invalid/qr"));
	}

	@Test
	void cus26SinStockSuficienteEs409YNoVende() throws Exception {
		cajaAbierta();
		Long idProducto = productoConStock("Mascarilla escasa", 1);
		long ventasAntes = id("SELECT COUNT(*) FROM VENTA_DETALLE WHERE id_producto = ?", idProducto);

		vender(null, idProducto, 2, "EFECTIVO", null).andExpect(status().isConflict()).andExpect(jsonPath("$.regla").value("RN15"));

		assertThat(id("SELECT COUNT(*) FROM VENTA_DETALLE WHERE id_producto = ?", idProducto)).isEqualTo(ventasAntes);
		assertThat(stock(idProducto)).isEqualTo(1);
	}

	// ------------------------------------------------------------------ CUS22

	@Test
	void cus22LaRecepcionistaSolicitaYUnaSegundaSolicitudEs409() throws Exception {
		Long idCliente = clientaNueva("Arrepentida");
		Long idVenta = ventaAceptada(idCliente, "EFECTIVO", null);

		Long idSolicitud = idDe(solicitar(idVenta, "SALDO_FAVOR").andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.idVenta").value(idVenta))
			.andExpect(jsonPath("$.idUsuarioSolicita").value(id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'recepcion'")))
			.andExpect(jsonPath("$.fechaSolicitud").exists()), "idSolicitud");
		assertThat(estadoVenta(idVenta)).isEqualTo("CERRADA");

		// UQ_SOL_ANUL_PENDIENTE: una sola PENDIENTE por venta
		solicitar(idVenta, "SALDO_FAVOR").andExpect(status().isConflict());

		// La bandeja de la Administradora y su aviso (CUS50); la recepcionista no ve la bandeja
		como("admin", get("/api/v1/solicitudes-anulacion"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idSolicitud", hasItem(idSolicitud.intValue())));
		como("admin", get("/api/v1/avisos"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].tipo", hasItem("ANULACION_PENDIENTE")));
		enviar(get("/api/v1/solicitudes-anulacion"), null).andExpect(status().isForbidden());

		// La cierra para no dejarla en la bandeja de las demás pruebas
		como("admin", post("/api/v1/solicitudes-anulacion/" + idSolicitud + "/rechazo"), "{\"motivoRechazo\":\"Prueba terminada\"}").andExpect(status().isOk());
	}

	@Test
	void cus22UnClienteDePasoNoEligeSaldoAFavorYUnaVentaAbiertaNoSeAnula() throws Exception {
		Long idVenta = ventaAceptada(null, "EFECTIVO", null);
		solicitar(idVenta, "SALDO_FAVOR").andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RN44"));

		Long abierta = idDe(crearVenta(atencionFinalizada(null, variante(CORTE))).andExpect(status().isCreated()), "idVenta");
		solicitar(abierta, "MEDIO_ORIGINAL").andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RN12"));
		assertThat(id("SELECT COUNT(*) FROM SOLICITUD_ANULACION WHERE id_venta IN (?, ?)", idVenta, abierta)).isZero();
	}

	// ------------------------------------------------------------------ CUS23

	@Test
	void cus23AprobarConSaldoAFavorAnulaConNotaDeCreditoYOracleAbona() throws Exception {
		Long idCliente = clientaNueva("Con saldo");
		Long idVenta = ventaAceptada(idCliente, "EFECTIVO", null);
		BigDecimal total = jdbc.queryForObject("SELECT total FROM VENTA WHERE id_venta = ?", BigDecimal.class, idVenta);
		Long idProducto = id("SELECT id_producto FROM VENTA_DETALLE WHERE id_venta = ?", idVenta);
		Long idSolicitud = idDe(solicitar(idVenta, "SALDO_FAVOR").andExpect(status().isCreated()), "idSolicitud");

		// RN32: solo la Administradora resuelve
		aprobar("recepcion", idSolicitud).andExpect(status().isForbidden());

		sunatAcepta();
		Long idNotaCredito = idDe(aprobar("admin", idSolicitud).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("APROBADA"))
			.andExpect(jsonPath("$.idNotaCredito").isNumber())
			.andExpect(jsonPath("$.fechaResolucion").exists()), "idNotaCredito");

		assertThat(estadoVenta(idVenta)).isEqualTo("ANULADA");
		assertThat(jdbc.queryForObject("SELECT tipo FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idNotaCredito)).isEqualTo("NC_BOLETA");
		assertThat(jdbc.queryForObject("SELECT serie FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idNotaCredito)).startsWith("BN");
		assertThat(jdbc.queryForObject("SELECT estado_sunat FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idNotaCredito)).isEqualTo("ACEPTADO");
		assertThat(jdbc.queryForObject("SELECT forma_devolucion FROM NOTA_CREDITO WHERE id_nota_credito = ?", String.class, idNotaCredito))
			.isEqualTo("SALDO_FAVOR");
		assertThat(id("SELECT id_usuario_autoriza FROM NOTA_CREDITO WHERE id_nota_credito = ?", idNotaCredito))
			.isEqualTo(id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'admin'"));
		// [It. 2.1 · D-21] TRG_NOTA_CREDITO_REGLAS toma de la solicitud a quien pidió la anulación: la recepcionista, no la Administradora
		assertThat(id("SELECT id_usuario_solicita FROM NOTA_CREDITO WHERE id_nota_credito = ?", idNotaCredito))
			.isEqualTo(id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'recepcion'"))
			.isEqualTo(id("SELECT id_usuario_solicita FROM SOLICITUD_ANULACION WHERE id_solicitud = ?", idSolicitud));
		assertThat(id("SELECT id_nota_credito FROM SOLICITUD_ANULACION WHERE id_solicitud = ?", idSolicitud)).isEqualTo(idNotaCredito);
		// PKG_VENTA.al_anular: el ABONO del total y el reingreso del producto los hace Oracle
		assertThat(jdbc.queryForObject("SELECT SUM(monto) FROM SALDO_FAVOR_MOV WHERE id_venta = ? AND tipo = 'ABONO'", BigDecimal.class, idVenta))
			.isEqualByComparingTo(total);
		assertThat(stock(idProducto)).isEqualTo(5);

		// Una solicitud resuelta no cambia
		aprobar("admin", idSolicitud).andExpect(status().isConflict()).andExpect(jsonPath("$.regla").value("RN32"));
	}

	@Test
	void cus23RechazarExigeMotivoYLaVentaSigueCerrada() throws Exception {
		Long idVenta = ventaAceptada(null, "EFECTIVO", null);
		Long idSolicitud = idDe(solicitar(idVenta, "MEDIO_ORIGINAL").andExpect(status().isCreated()), "idSolicitud");

		como("admin", post("/api/v1/solicitudes-anulacion/" + idSolicitud + "/rechazo"), "{}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN32"));
		como("admin", post("/api/v1/solicitudes-anulacion/" + idSolicitud + "/rechazo"), "{\"motivoRechazo\":\"La venta es correcta\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("RECHAZADA"))
			.andExpect(jsonPath("$.motivoRechazo").value("La venta es correcta"));

		assertThat(estadoVenta(idVenta)).isEqualTo("CERRADA");
		como("admin", get("/api/v1/solicitudes-anulacion").param("estado", "RECHAZADA"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idSolicitud", hasItem(idSolicitud.intValue())));
	}

	@Test
	void cus23UnComprobanteQueSunatAunNoAceptaEs409() throws Exception {
		seriesDeNotaCredito();
		cajaAbierta();
		Long idVenta = idDe(vender(null, productoConStock("Sin aceptar", 2), 1, "EFECTIVO", null).andExpect(status().isCreated()), "idVenta");
		Long idSolicitud = idDe(solicitar(idVenta, "MEDIO_ORIGINAL").andExpect(status().isCreated()), "idSolicitud");

		aprobar("admin", idSolicitud).andExpect(status().isConflict()).andExpect(jsonPath("$.regla").value("RN46"));

		assertThat(estadoVenta(idVenta)).isEqualTo("CERRADA");
		assertThat(jdbc.queryForObject("SELECT estado FROM SOLICITUD_ANULACION WHERE id_solicitud = ?", String.class, idSolicitud)).isEqualTo("PENDIENTE");
		como("admin", post("/api/v1/solicitudes-anulacion/" + idSolicitud + "/rechazo"), "{\"motivoRechazo\":\"Prueba terminada\"}").andExpect(status().isOk());
	}

	// ------------------------------------------------------------------ CUS24

	@Test
	void cus24DevuelveLaTarjetaPorLaPasarelaSinTocarElSaldoAFavor() throws Exception {
		Long idCliente = clientaNueva("Con tarjeta");
		Long idVenta = ventaAceptada(idCliente, "TARJETA", "POS");
		Long idCobro = id("SELECT id_transaccion FROM VENTA_PAGO WHERE id_venta = ?", idVenta);
		Long idSolicitud = idDe(solicitar(idVenta, "MEDIO_ORIGINAL").andExpect(status().isCreated()), "idSolicitud");
		sunatAcepta();
		Long idNotaCredito = idDe(aprobar("admin", idSolicitud).andExpect(status().isOk()), "idNotaCredito");

		// Solo la Administradora devuelve
		enviar(post("/api/v1/ventas/" + idVenta + "/devolucion"), "{\"formaDevolucion\":\"MEDIO_ORIGINAL\"}").andExpect(status().isForbidden());

		como("admin", post("/api/v1/ventas/" + idVenta + "/devolucion"), "{\"formaDevolucion\":\"MEDIO_ORIGINAL\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.idNotaCredito").value(idNotaCredito))
			.andExpect(jsonPath("$.devoluciones", hasSize(1)))
			.andExpect(jsonPath("$.devoluciones[0].estado").value("APROBADA"));

		assertThat(id("SELECT COUNT(*) FROM TRANSACCION_PASARELA WHERE tipo_operacion = 'DEVOLUCION' AND estado = 'APROBADA' AND id_transaccion_origen = ?"
				+ " AND id_nota_credito = ?", idCobro, idNotaCredito)).isEqualTo(1);
		assertThat(id("SELECT COUNT(*) FROM SALDO_FAVOR_MOV WHERE id_venta = ? AND tipo = 'ABONO'", idVenta)).isZero();

		// Lo que ya se devolvió no se devuelve otra vez (TRG_TRANS_PAS_REGLAS, -20077)
		como("admin", post("/api/v1/ventas/" + idVenta + "/devolucion"), "{\"formaDevolucion\":\"MEDIO_ORIGINAL\"}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RN44"));
	}

	@Test
	void cus24RegistraLaDevolucionManualDeYapeConSuNumeroDeOperacion() throws Exception {
		Long idVenta = ventaAceptada(null, "YAPE", "QR");
		Long idCobro = id("SELECT id_transaccion FROM VENTA_PAGO WHERE id_venta = ?", idVenta);
		Long idSolicitud = idDe(solicitar(idVenta, "MEDIO_ORIGINAL").andExpect(status().isCreated()), "idSolicitud");
		sunatAcepta();
		Long idNotaCredito = idDe(aprobar("admin", idSolicitud).andExpect(status().isOk()), "idNotaCredito");

		// Yape por QR no va por la pasarela
		como("admin", post("/api/v1/ventas/" + idVenta + "/devolucion"), "{\"formaDevolucion\":\"MEDIO_ORIGINAL\"}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN44"));

		String operacion = "YAPE-" + idVenta;
		como("admin", post("/api/v1/ventas/" + idVenta + "/devolucion-manual"), "{\"idTransaccion\":" + idCobro + ",\"codigoOperacion\":\"" + operacion + "\"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.devoluciones[0].estado").value("APROBADA"))
			.andExpect(jsonPath("$.devoluciones[0].canal").value("QR"))
			.andExpect(jsonPath("$.devoluciones[0].codigoOperacion").value(operacion));

		assertThat(id("SELECT COUNT(*) FROM TRANSACCION_PASARELA WHERE tipo_operacion = 'DEVOLUCION' AND canal = 'QR' AND estado = 'APROBADA'"
				+ " AND codigo_operacion = ? AND id_transaccion_origen = ? AND id_nota_credito = ?", operacion, idCobro, idNotaCredito)).isEqualTo(1);
		assertThat(id("SELECT COUNT(*) FROM SALDO_FAVOR_MOV WHERE id_venta = ?", idVenta)).isZero();
	}

}
