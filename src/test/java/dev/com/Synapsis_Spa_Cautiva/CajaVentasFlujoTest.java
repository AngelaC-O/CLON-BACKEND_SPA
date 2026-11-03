package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Flujos de CUS44, CUS45, CUS18, CUS19, CUS20, CUS21, CUS13 y CUS07 con los falsos: Izipay aprueba y APISPERU responde PENDIENTE. */
@SpringBootTest
@AutoConfigureMockMvc
class CajaVentasFlujoTest extends VentasFlujoApoyo {

	private static final String CLAVE_HMAC = "clave-hmac-solo-de-pruebas";

	private String estadoDe(String tabla, String columnaId, Long id) {
		return jdbc.queryForObject("SELECT estado FROM " + tabla + " WHERE " + columnaId + " = ?", String.class, id);
	}

	/** El primer número de un filtro de JsonPath: Oracle devuelve 35 o 35.5 según el valor, entero o decimal. */
	private double numero(ResultActions resultado, String ruta) throws Exception {
		List<Number> valores = JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), ruta);
		return valores.getFirst().doubleValue();
	}

	// ------------------------------------------------------------------ CUS44

	@Test
	void cus44AbreLaCajaYUnaSegundaEnLaMismaSedeEs409() throws Exception {
		Long idSede = sedeNueva("Caja");

		enviarEnSede("recepcion", idSede, post("/api/v1/cajas"), "{\"montoInicial\":150.50}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("ABIERTA"))
			.andExpect(jsonPath("$.idSede").value(idSede))
			.andExpect(jsonPath("$.montoInicial").value(150.50));

		enviarEnSede("recepcion", idSede, post("/api/v1/cajas"), "{\"montoInicial\":10}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RN30"));
		assertThat(id("SELECT COUNT(*) FROM CAJA WHERE id_sede = ?", idSede)).isEqualTo(1);
	}

	@Test
	void cus44LaEspecialistaNoAbreCaja() throws Exception {
		enviarEnSede("especialista1", sedeNueva("Sin permiso"), post("/api/v1/cajas"), "{\"montoInicial\":10}").andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ CUS45

	private String arqueoCompleto(String efectivoContado, String observacion) {
		String conObservacion = observacion == null ? "" : ",\"observacion\":\"" + observacion + "\"";
		return "\"arqueos\":[{\"idMedioPago\":" + medio("EFECTIVO") + ",\"montoContado\":" + efectivoContado + conObservacion + "},{\"idMedioPago\":" + medio("YAPE")
				+ ",\"montoContado\":0},{\"idMedioPago\":" + medio("PLIN") + ",\"montoContado\":0},{\"idMedioPago\":" + medio("TARJETA") + ",\"montoContado\":0}]";
	}

	@Test
	void cus45MuestraElEsperadoYCierraConEgresoYDiferenciaObservada() throws Exception {
		Long idSede = sedeNueva("Cierre");
		Long idCaja = idDe(enviarEnSede("recepcion", idSede, post("/api/v1/cajas"), "{\"montoInicial\":100}").andExpect(status().isCreated()), "idCaja");

		ResultActions esperado = enviarEnSede("recepcion", idSede, get("/api/v1/cajas/" + idCaja + "/esperado"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.medios", hasSize(4)))
			.andExpect(jsonPath("$.comprobantesPendientes", hasSize(0)));
		assertThat(numero(esperado, "$.medios[?(@.medioPago == 'EFECTIVO')].esperado")).isEqualTo(100.0);

		ResultActions cierre = enviarEnSede("recepcion", idSede, post("/api/v1/cajas/" + idCaja + "/cierre"),
				"{\"egresos\":[{\"concepto\":\"Compra menor\",\"monto\":10}]," + arqueoCompleto("85", "Faltan cinco soles") + "}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CERRADA"))
			.andExpect(jsonPath("$.idUsuarioCierre", notNullValue()));
		assertThat(numero(cierre, "$.arqueos[?(@.idMedioPago == " + medio("EFECTIVO") + ")].montoEsperado")).isEqualTo(90.0);
		assertThat(numero(cierre, "$.arqueos[?(@.idMedioPago == " + medio("EFECTIVO") + ")].diferencia")).isEqualTo(-5.0);
		assertThat(id("SELECT COUNT(*) FROM CAJA_EGRESO WHERE id_caja = ?", idCaja)).isEqualTo(1);
	}

	@Test
	void cus45UnaDiferenciaSinObservacionOUnMedioSinArqueoEs422YLaCajaSigueAbierta() throws Exception {
		Long idSede = sedeNueva("Cierre con error");
		Long idCaja = idDe(enviarEnSede("recepcion", idSede, post("/api/v1/cajas"), "{\"montoInicial\":100}").andExpect(status().isCreated()), "idCaja");

		enviarEnSede("recepcion", idSede, post("/api/v1/cajas/" + idCaja + "/cierre"), "{" + arqueoCompleto("85", null) + "}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(containsString("RN30")));
		enviarEnSede("recepcion", idSede, post("/api/v1/cajas/" + idCaja + "/cierre"),
				"{\"arqueos\":[{\"idMedioPago\":" + medio("EFECTIVO") + ",\"montoContado\":100}]}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF64"));

		assertThat(estadoDe("CAJA", "id_caja", idCaja)).isEqualTo("ABIERTA");
	}

	@Test
	void cus45UnaCajaInexistenteEs404() throws Exception {
		enviar(get("/api/v1/cajas/999999/esperado"), null).andExpect(status().isNotFound());
		enviar(post("/api/v1/cajas/999999/cierre"), "{" + arqueoCompleto("0", null) + "}").andExpect(status().isNotFound());
	}

	@Test
	void cus45ElEsperadoSumaLaVentaCerradaLosAprobadosYAvisaLosPendientes() throws Exception {
		Long idCaja = cajaAbierta();
		Long idVenta = idDe(crearVenta(atencionFinalizada(null, variante(CORTE))).andExpect(status().isCreated()), "idVenta");
		pagar(idVenta, medio("YAPE"), PRECIO_CORTE.toPlainString(), "QR").andExpect(status().isOk());
		ResultActions cerrada = cerrarConBoleta(idVenta).andExpect(status().isOk());
		Long idComprobante = idDe(cerrada, "comprobante.idComprobante");

		ResultActions esperado = enviar(get("/api/v1/cajas/" + idCaja + "/esperado"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.comprobantesPendientes[*].idComprobante", hasItem(idComprobante.intValue())));
		assertThat(numero(esperado, "$.medios[?(@.medioPago == 'YAPE')].esperado")).isEqualTo(esperadoYape(idCaja).doubleValue()).isGreaterThanOrEqualTo(35.0);
		assertThat(numero(esperado, "$.medios[?(@.medioPago == 'YAPE')].aprobadoPasarela")).isGreaterThanOrEqualTo(35.0);
	}

	private BigDecimal esperadoYape(Long idCaja) {
		return jdbc.queryForObject("SELECT esperado FROM V_CAJA_ESPERADO WHERE id_caja = ? AND medio_pago = 'YAPE'", BigDecimal.class, idCaja);
	}

	// ------------------------------------------------------------------ CUS18 y CUS19

	@Test
	void cus18CreaDescuentaCobraYCierraYOracleMarcaLaAtencionCobrada() throws Exception {
		Long idCaja = cajaAbierta();
		Long idCliente = clienta("Cobrada");
		Long idAtencion = atencionFinalizada(idCliente, variante(CORTE));

		ResultActions creada = crearVenta(idAtencion).andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("ABIERTA"))
			.andExpect(jsonPath("$.idCaja").value(idCaja))
			.andExpect(jsonPath("$.idCliente").value(idCliente))
			.andExpect(jsonPath("$.detalles[0].idAtencion").value(idAtencion))
			.andExpect(jsonPath("$.detalles[0].precioUnitario").value(35.0))
			.andExpect(jsonPath("$.detalles[0].idPersonal").value(especialista()))
			.andExpect(jsonPath("$.total").value(35.0));
		Long idVenta = idDe(creada, "idVenta");

		enviar(patch("/api/v1/ventas/" + idVenta + "/descuentos"), "{}").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(35.0));

		enviar(post("/api/v1/ventas/" + idVenta + "/pagos"), "{\"pagos\":[{\"idMedioPago\":" + medio("EFECTIVO") + ",\"monto\":20},{\"idMedioPago\":" + medio("YAPE")
				+ ",\"monto\":15,\"canal\":\"QR\"}]}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pagos", hasSize(2)))
			.andExpect(jsonPath("$.pagado").value(35.0))
			.andExpect(jsonPath("$.pendiente").value(0.0))
			.andExpect(jsonPath("$.pagos[1].idTransaccion", notNullValue()))
			.andExpect(jsonPath("$.pagos[1].referencia", notNullValue()));

		cerrarConBoleta(idVenta).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CERRADA"))
			.andExpect(jsonPath("$.comprobante.tipo").value("BOLETA"))
			.andExpect(jsonPath("$.comprobante.serie", notNullValue()))
			.andExpect(jsonPath("$.comprobante.numero", notNullValue()))
			.andExpect(jsonPath("$.comprobante.estadoSunat").value("PENDIENTE"));

		assertThat(estadoDe("ATENCION", "id_atencion", idAtencion)).isEqualTo("COBRADA");
		assertThat(jdbc.queryForObject("SELECT estado FROM TRANSACCION_PASARELA t JOIN VENTA_PAGO p ON p.id_transaccion = t.id_transaccion WHERE p.id_venta = ?",
				String.class, idVenta)).isEqualTo("APROBADA");
		assertThat(id("SELECT intentos_envio FROM COMPROBANTE WHERE id_venta = ?", idVenta)).isEqualTo(1);

		// La atención ya cobrada no se vuelve a cobrar
		crearVenta(idAtencion).andExpect(status().isConflict()).andExpect(jsonPath("$.regla").value("RF21"));
	}

	@Test
	void cus18CrearSinCajaAbiertaEs422YSinAtencionFinalizadaTambien() throws Exception {
		Long idSedeSinCaja = sedeNueva("Sin caja");
		enviarEnSede("recepcion", idSedeSinCaja, post("/api/v1/ventas"), "{\"idAtencion\":1}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN30"));

		cajaAbierta();
		crearVenta(999_999L).andExpect(status().isNotFound());
	}

	@Test
	void cus18ElDescuentoAplicaLaPromocionSoloSiOracleLaAceptaYElSaldoSiAlcanza() throws Exception {
		cajaAbierta();
		Long idCliente = clienta("Sin saldo");
		Long idVenta = idDe(crearVenta(atencionFinalizada(idCliente, variante(CORTE))).andExpect(status().isCreated()), "idVenta");
		Long idUsuario = id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'admin'");
		comoDueno("INSERT INTO PROMOCION (nombre, tipo_descuento, valor, fecha_inicio, fecha_fin, id_usuario) VALUES ('Sin servicios', 'PORCENTAJE', 10, TRUNC(SYSDATE) - 1, TRUNC(SYSDATE) + 1, "
				+ idUsuario + ")");
		Long idPromocion = id("SELECT MAX(id_promocion) FROM PROMOCION");

		// PROMOCION_SERVICIO no es de esta iteración: ninguna promoción aplica a un servicio (-20047)
		enviar(patch("/api/v1/ventas/" + idVenta + "/descuentos"), "{\"idPromocion\":" + idPromocion + "}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("ORA-20047"));
		enviar(patch("/api/v1/ventas/" + idVenta + "/descuentos"), "{\"saldoFavorAplicado\":5}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("ORA-20064"));
		enviar(patch("/api/v1/ventas/999999/descuentos"), "{}").andExpect(status().isNotFound());
	}

	@Test
	void cus18AplicaElAdelantoDeLaCitaYElSaldoAFavor() throws Exception {
		cajaAbierta();
		Long idCliente = clienta("Con adelanto");
		LocalDateTime inicio = lunes(20).atTime(10, 0);
		Long idVarianteCorte = variante(CORTE);
		Long idCita = idDe(agendar(idCliente, recurso(1), inicio).andExpect(status().isCreated()), "idCita");
		enviar(post("/api/v1/citas/" + idCita + "/adelanto"), "{\"adelantoMonto\":10,\"idMedioPago\":" + medio("EFECTIVO") + "}").andExpect(status().isOk());
		// Otra cita cancelada deja saldo a favor: el abono lo admite Oracle solo con el adelanto de una cita CANCELADA
		Long idCitaCancelada = idDe(agendar(idCliente, recurso(2), inicio.plusHours(2)).andExpect(status().isCreated()), "idCita");
		enviar(post("/api/v1/citas/" + idCitaCancelada + "/adelanto"), "{\"adelantoMonto\":5,\"idMedioPago\":" + medio("EFECTIVO") + "}").andExpect(status().isOk());
		comoDueno("UPDATE CITA SET estado = 'CANCELADA', motivo_cancelacion = 'De prueba' WHERE id_cita = " + idCitaCancelada);
		comoDueno("INSERT INTO SALDO_FAVOR_MOV (id_cliente, id_cita, tipo, monto, fecha) VALUES (" + idCliente + ", " + idCitaCancelada + ", 'ABONO', 5, SYSTIMESTAMP)");
		Long idAtencion = atencionDeCita(idCita, idCliente, idVarianteCorte);

		Long idVenta = idDe(crearVenta(idAtencion).andExpect(status().isCreated()), "idVenta");
		enviar(patch("/api/v1/ventas/" + idVenta + "/descuentos"), "{\"saldoFavorAplicado\":5}").andExpect(status().isOk())
			.andExpect(jsonPath("$.adelantoAplicado").value(10.0))
			.andExpect(jsonPath("$.saldoFavorAplicado").value(5.0))
			.andExpect(jsonPath("$.total").value(20.0));

		// Un segundo uso no coincidiría con lo aplicado
		enviar(patch("/api/v1/ventas/" + idVenta + "/descuentos"), "{\"saldoFavorAplicado\":5}").andExpect(status().isUnprocessableContent());

		pagar(idVenta, medio("EFECTIVO"), "20", null).andExpect(status().isOk());
		cerrarConBoleta(idVenta).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(20.0));
	}

	private ResultActions agendar(Long idCliente, Long idRecurso, LocalDateTime inicio) throws Exception {
		return enviar(post("/api/v1/citas"), "{\"idCliente\":" + idCliente + ",\"idPersonal\":" + especialista() + ",\"idRecurso\":" + idRecurso + ",\"idVariante\":"
				+ variante(CORTE) + ",\"fechaHoraInicio\":\"" + inicio + "\",\"duracionMin\":30}");
	}

	/** La cita pasa por EN_ATENCION y FINALIZADA como en CUS11, en un día pasado para no ocupar a la especialista hoy. */
	private Long atencionDeCita(Long idCita, Long idCliente, Long idVariante) throws Exception {
		Long idRecurso = id("SELECT id_recurso FROM CITA WHERE id_cita = ?", idCita);
		comoDueno("UPDATE CITA SET estado = 'EN_ATENCION' WHERE id_cita = " + idCita);
		comoDueno("INSERT INTO ATENCION (id_cita, id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES (" + idCita + ", " + idCliente + ", "
				+ especialista() + ", " + idRecurso + ", " + idVariante + ", CAST(SYSTIMESTAMP AS TIMESTAMP) - 500, 30)");
		Long idAtencion = id("SELECT id_atencion FROM ATENCION WHERE id_cita = ?", idCita);
		comoDueno("UPDATE ATENCION SET estado = 'FINALIZADA', hora_fin = hora_inicio + INTERVAL '30' MINUTE WHERE id_atencion = " + idAtencion);
		comoDueno("UPDATE CITA SET estado = 'FINALIZADA' WHERE id_cita = " + idCita);
		return idAtencion;
	}

	@Test
	void cus18PagosDistintosDelTotalEs422YNoSeCierra() throws Exception {
		cajaAbierta();
		Long idVenta = idDe(crearVenta(atencionFinalizada(null, variante(CORTE))).andExpect(status().isCreated()), "idVenta");
		pagar(idVenta, medio("EFECTIVO"), "10", null).andExpect(status().isOk()).andExpect(jsonPath("$.pendiente").value(25.0));

		cerrarConBoleta(idVenta).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RN11"));

		assertThat(estadoDe("VENTA", "id_venta", idVenta)).isEqualTo("ABIERTA");
		assertThat(id("SELECT COUNT(*) FROM COMPROBANTE WHERE id_venta = ?", idVenta)).isZero();
	}

	@Test
	void cus18UnMedioInexistenteEs422YUnaVentaInexistente404() throws Exception {
		cajaAbierta();
		Long idVenta = idDe(crearVenta(atencionFinalizada(null, variante(CORTE))).andExpect(status().isCreated()), "idVenta");

		pagar(idVenta, 999_999L, "10", null).andExpect(status().isUnprocessableContent());
		pagar(999_999L, medio("EFECTIVO"), "10", null).andExpect(status().isNotFound());
		pagar(idVenta, medio("YAPE"), "10", null).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RF26"));
	}

	@Test
	void cus19ConsultaElRucYSigueAManoSiFalla() throws Exception {
		enviar(get("/api/v1/ventas/consulta-ruc/20601234567"), null).andExpect(status().isAccepted()).andExpect(jsonPath("$.resultado").value("ERROR"));
		enviar(get("/api/v1/ventas/consulta-ruc/123"), null).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RN45"));
	}

	@Test
	void cus19FacturaConRucSeEmiteConLaSerieDeOracleYConDniEs422() throws Exception {
		cajaAbierta();
		Long idRuc = id("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE codigo_sunat = '6'");
		Long idDni = id("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'");
		Long idVenta = idDe(crearVenta(atencionFinalizada(null, variante(CORTE))).andExpect(status().isCreated()), "idVenta");
		pagar(idVenta, medio("EFECTIVO"), "35", null).andExpect(status().isOk());

		enviar(post("/api/v1/ventas/" + idVenta + "/cierre"),
				"{\"tipo\":\"FACTURA\",\"idTipoDocComprador\":" + idDni + ",\"nroDocComprador\":\"71234567\",\"nombreComprador\":\"Persona\"}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN08"));
		enviar(post("/api/v1/ventas/" + idVenta + "/cierre"), "{\"tipo\":\"FACTURA\"}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN08"));

		enviar(post("/api/v1/ventas/" + idVenta + "/cierre"),
				"{\"tipo\":\"FACTURA\",\"idTipoDocComprador\":" + idRuc + ",\"nroDocComprador\":\"20601234567\",\"nombreComprador\":\"EMPRESA DEMO S.A.C.\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.comprobante.tipo").value("FACTURA"))
			.andExpect(jsonPath("$.comprobante.serie").value(containsString("F")))
			.andExpect(jsonPath("$.comprobante.fechaEmision").value(LocalDate.now().toString()));
	}

	@Test
	void cus19UnaBoletaSobreElUmbralPideComprador() throws Exception {
		cajaAbierta();
		Long idArea = id("SELECT MIN(id_area) FROM AREA WHERE nombre LIKE 'Peluquer%'");
		comoDueno("INSERT INTO SERVICIO (id_area, nombre, duracion_ref_min, tipo_recurso) VALUES (" + idArea + ", 'Novia completa', 120, 'SILLON')");
		comoDueno("INSERT INTO VARIANTE_SERVICIO (id_servicio, nombre, precio) SELECT id_servicio, 'Estándar', 800 FROM SERVICIO WHERE nombre = 'Novia completa'");
		Long idVenta = idDe(crearVenta(atencionFinalizada(null, variante("Novia completa"))).andExpect(status().isCreated()), "idVenta");
		pagar(idVenta, medio("EFECTIVO"), "800", null).andExpect(status().isOk());

		cerrarConBoleta(idVenta).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RN08"));
	}

	// ------------------------------------------------------------------ CUS20

	@Test
	void cus20EntregaLaRepresentacionYElEnlace() throws Exception {
		Long idComprobante = idDe(ventaCerrada(), "comprobante.idComprobante");

		enviar(get("/api/v1/comprobantes/" + idComprobante + "/representacion"), null).andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_PDF));
		enviar(get("/api/v1/comprobantes/" + idComprobante + "/enlace").param("canal", "CORREO").param("correo", "clienta@ejemplo.com"), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enlace").value(containsString("/comprobantes/publico/" + idComprobante + "?expira=")))
			.andExpect(jsonPath("$.correo").value("clienta@ejemplo.com"));
	}

	@Test
	void cus20UnComprobanteInexistenteEs404() throws Exception {
		enviar(get("/api/v1/comprobantes/999999/representacion"), null).andExpect(status().isNotFound());
		enviar(get("/api/v1/comprobantes/999999/enlace"), null).andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------------ CUS21

	@Test
	void cus21ArmaLaProformaSinGuardarNada() throws Exception {
		Long ventasAntes = id("SELECT COUNT(*) FROM VENTA");

		enviar(post("/api/v1/proformas"), "{\"nombreCliente\":\"Novia\",\"fechaValidez\":\"" + lunes(4) + "\",\"items\":[{\"tipoItem\":\"SERVICIO\",\"idVariante\":" + variante(CORTE)
				+ ",\"cantidad\":2}]}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].precioUnitario").value(35.0))
			.andExpect(jsonPath("$.items[0].subtotal").value(70.0))
			.andExpect(jsonPath("$.total").value(70.0));

		assertThat(id("SELECT COUNT(*) FROM VENTA")).isEqualTo(ventasAntes);
	}

	@Test
	void cus21SinItemsEs400YUnaVarianteInexistente404() throws Exception {
		enviar(post("/api/v1/proformas"), "{\"items\":[]}").andExpect(status().isBadRequest());
		enviar(post("/api/v1/proformas"), "{\"items\":[{\"tipoItem\":\"SERVICIO\",\"idVariante\":999999,\"cantidad\":1}]}").andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------------ CUS13

	private String cuerpoRapida(Long idPersonal, Long idRecurso, String monto) {
		return "{\"nombreClientePaso\":\"De paso\",\"idPersonal\":" + idPersonal + ",\"idRecurso\":" + idRecurso + ",\"idVariante\":" + variante(CORTE)
				+ ",\"tiempoDeclaradoMin\":30,\"idMedioPago\":" + medio("EFECTIVO") + ",\"monto\":" + monto + ",\"tipo\":\"BOLETA\"}";
	}

	@Test
	void cus13RegistraCobraYEmiteEnUnaOperacionYSiFallaNoDejaNada() throws Exception {
		cajaAbierta();
		Long idEspecialista = especialista();
		Long idRecurso = recurso(1);

		enviar(get("/api/v1/ventas/rapidas/libres").param("idVariante", variante(CORTE).toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].idsPersonal", hasItem(idEspecialista.intValue())))
			.andExpect(jsonPath("$[0].idsRecurso", hasItem(idRecurso.intValue())));

		// Pago distinto del total: se revierte también la atención
		Long atencionesAntes = id("SELECT COUNT(*) FROM ATENCION");
		enviar(post("/api/v1/ventas/rapidas"), cuerpoRapida(idEspecialista, idRecurso, "10")).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN11"));
		assertThat(id("SELECT COUNT(*) FROM ATENCION")).isEqualTo(atencionesAntes);

		ResultActions cobrada = enviar(post("/api/v1/ventas/rapidas"), cuerpoRapida(idEspecialista, idRecurso, "35")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("CERRADA"))
			.andExpect(jsonPath("$.comprobante.estadoSunat").value("PENDIENTE"));
		Long idAtencion = id("SELECT d.id_atencion FROM VENTA_DETALLE d WHERE d.id_venta = ?", idDe(cobrada, "idVenta"));
		assertThat(estadoDe("ATENCION", "id_atencion", idAtencion)).isEqualTo("COBRADA");
		// El bloque de la agenda sigue ocupando el tiempo declarado (DF-37)
		assertThat(id("SELECT ROUND((CAST(hora_fin AS DATE) - CAST(hora_inicio AS DATE)) * 1440) FROM ATENCION WHERE id_atencion = ?", idAtencion)).isEqualTo(30);

		// La especialista sigue ocupada: cruce (RN34)
		enviar(post("/api/v1/ventas/rapidas"), cuerpoRapida(idEspecialista, recurso(2), "35")).andExpect(status().isConflict()).andExpect(jsonPath("$.regla").value("RN34"));
	}

	@Test
	void cus13SinVarianteEs400() throws Exception {
		enviar(get("/api/v1/ventas/rapidas/libres"), null).andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------ CUS07

	private String firma(String cuerpo) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(CLAVE_HMAC.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
	}

	private ResultActions notificar(String cuerpo, String firma) throws Exception {
		return mvc.perform(post("/api/v1/pagos/notificaciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo).header("X-Firma-Hmac", firma));
	}

	@Test
	void cus07ElAdelantoEnEfectivoEntraALaCajaAbierta() throws Exception {
		Long idCaja = cajaAbierta();
		Long idCita = idDe(agendar(clienta("Adelanto efectivo"), recurso(0), lunes(21).atTime(10, 0)).andExpect(status().isCreated()), "idCita");

		enviar(post("/api/v1/citas/" + idCita + "/adelanto"), "{\"adelantoMonto\":25,\"idMedioPago\":" + medio("EFECTIVO") + "}").andExpect(status().isOk())
			.andExpect(jsonPath("$.adelantoMonto").value(25.0))
			.andExpect(jsonPath("$.idCaja").value(idCaja));
	}

	@Test
	void cus07EfectivoConMedioDigitalEs422YCitaInexistente404() throws Exception {
		cajaAbierta();
		Long idCita = idDe(agendar(clienta("Adelanto errado"), recurso(0), lunes(22).atTime(10, 0)).andExpect(status().isCreated()), "idCita");

		enviar(post("/api/v1/citas/" + idCita + "/adelanto"), "{\"adelantoMonto\":25,\"idMedioPago\":" + medio("YAPE") + "}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN42"));
		enviar(post("/api/v1/citas/999999/adelanto"), "{\"adelantoMonto\":25,\"idMedioPago\":" + medio("EFECTIVO") + "}").andExpect(status().isNotFound());
		enviar(post("/api/v1/citas/999999/enlace-adelanto"), "{\"adelantoMonto\":25,\"idMedioPago\":" + medio("YAPE") + "}").andExpect(status().isNotFound());
		enviar(post("/api/v1/citas/" + idCita + "/enlace-adelanto"), "{\"adelantoMonto\":25,\"idMedioPago\":" + medio("EFECTIVO") + "}")
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void cus07ElEnlaceSoloRegistraElAdelantoAlAprobarseYElWebhookRepetidoNoTieneEfecto() throws Exception {
		Long idCaja = cajaAbierta();
		Long idCita = idDe(agendar(clienta("Adelanto por enlace"), recurso(0), lunes(23).atTime(10, 0)).andExpect(status().isCreated()), "idCita");

		ResultActions enlace = enviar(post("/api/v1/citas/" + idCita + "/enlace-adelanto"), "{\"adelantoMonto\":50,\"idMedioPago\":" + medio("TARJETA") + "}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.canal").value("LINK"))
			.andExpect(jsonPath("$.enlacePago", notNullValue()))
			.andExpect(jsonPath("$.fechaExpiracion", notNullValue()));
		Long idTransaccion = idDe(enlace, "idTransaccion");

		// RN04: aún no hay adelanto
		assertThat(id("SELECT adelanto_monto FROM CITA WHERE id_cita = ?", idCita)).isZero();

		String codigo = jdbc.queryForObject("SELECT codigo_operacion FROM TRANSACCION_PASARELA WHERE id_transaccion = ?", String.class, idTransaccion);
		String cuerpo = "{\"codigoOperacion\":\"" + codigo + "\",\"estado\":\"APROBADA\"}";

		notificar(cuerpo, "firma-falsa").andExpect(status().isUnauthorized());
		assertThat(estadoDe("TRANSACCION_PASARELA", "id_transaccion", idTransaccion)).isEqualTo("PENDIENTE");

		notificar(cuerpo, firma(cuerpo)).andExpect(status().isOk());
		assertThat(estadoDe("TRANSACCION_PASARELA", "id_transaccion", idTransaccion)).isEqualTo("APROBADA");
		assertThat(id("SELECT adelanto_monto FROM CITA WHERE id_cita = ?", idCita)).isEqualTo(50);
		assertThat(id("SELECT id_caja FROM CITA WHERE id_cita = ?", idCita)).isEqualTo(idCaja);
		LocalDateTime fechaAdelanto = jdbc.queryForObject("SELECT fecha_adelanto FROM CITA WHERE id_cita = ?", LocalDateTime.class, idCita);

		// Repetido: 200 y nada cambia
		notificar(cuerpo, firma(cuerpo)).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("SELECT fecha_adelanto FROM CITA WHERE id_cita = ?", LocalDateTime.class, idCita)).isEqualTo(fechaAdelanto);
		assertThat(id("SELECT COUNT(*) FROM TRANSACCION_PASARELA WHERE id_cita = ?", idCita)).isEqualTo(1);

		String desconocido = "{\"codigoOperacion\":\"NO-EXISTE\",\"estado\":\"APROBADA\"}";
		notificar(desconocido, firma(desconocido)).andExpect(status().isNotFound());
	}

	@Test
	void cus07ElWebhookRechazadoNoRegistraAdelanto() throws Exception {
		Long idCita = idDe(agendar(clienta("Enlace rechazado"), recurso(0), lunes(24).atTime(10, 0)).andExpect(status().isCreated()), "idCita");
		Long idTransaccion = idDe(enviar(post("/api/v1/citas/" + idCita + "/enlace-adelanto"), "{\"adelantoMonto\":50,\"idMedioPago\":" + medio("YAPE") + "}")
			.andExpect(status().isCreated()), "idTransaccion");
		String codigo = jdbc.queryForObject("SELECT codigo_operacion FROM TRANSACCION_PASARELA WHERE id_transaccion = ?", String.class, idTransaccion);
		String cuerpo = "{\"codigoOperacion\":\"" + codigo + "\",\"estado\":\"RECHAZADA\"}";

		notificar(cuerpo, firma(cuerpo)).andExpect(status().isOk());

		assertThat(estadoDe("TRANSACCION_PASARELA", "id_transaccion", idTransaccion)).isEqualTo("RECHAZADA");
		assertThat(id("SELECT adelanto_monto FROM CITA WHERE id_cita = ?", idCita)).isZero();
	}

}
