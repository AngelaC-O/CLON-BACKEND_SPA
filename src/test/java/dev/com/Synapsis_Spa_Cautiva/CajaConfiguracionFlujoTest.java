package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * [It. 2] CUS46 (aprobar y reabrir caja), CUS47 (liquidación y conciliación), CUS54, CUS55, CUS62, CUS63 y CUS65, en la sede propia de la clase
 * (VentasFlujoApoyo). Aprobar y reabrir caja y conciliar usan permisos de CONCILIACION: la recepcionista, que tiene CAJA:EDITAR para cerrar su
 * caja, recibe 403. La conciliación es de toda la empresa y del mes actual: junta lo que dejaron las demás clases, así que la prueba justifica
 * todas sus diferencias antes de cerrarla.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CajaConfiguracionFlujoTest extends VentasFlujoApoyo {

	private static final String TOKEN_CSRF = "token-de-prueba";

	private ResultActions como(String usuario, MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		return enviarEnSede(usuario, sedeDeLaClase(), pedido, cuerpo);
	}

	/** CUS45: cierra la caja con el efectivo contado (si no coincide, con su observación) y los demás medios en 0, porque se arquean todos (RF64). */
	private ResultActions cerrarCaja(Long idCaja, String contado) throws Exception {
		StringBuilder arqueos = new StringBuilder("{\"idMedioPago\":" + medio("EFECTIVO") + ",\"montoContado\":" + contado
				+ ",\"observacion\":\"Diferencia de prueba\"}");
		for (Long idMedioPago : jdbc.queryForList("SELECT id_medio_pago FROM MEDIO_PAGO WHERE nombre <> 'EFECTIVO'", Long.class)) {
			arqueos.append(",{\"idMedioPago\":").append(idMedioPago).append(",\"montoContado\":0}");
		}
		return enviar(post("/api/v1/cajas/" + idCaja + "/cierre"), "{\"arqueos\":[" + arqueos + "]}");
	}

	/** El multipart del reporte, con la sesión y el CSRF como los manda Angular (multipart() no es un MockHttpServletRequestBuilder). */
	private ResultActions importar(String usuario, MockMultipartFile archivo) throws Exception {
		return mvc.perform(multipart("/api/v1/conciliaciones/liquidacion").file(archivo)
			.with(authentication(sesion(usuario, sedeDeLaClase())))
			.cookie(new Cookie("XSRF-TOKEN", TOKEN_CSRF))
			.header("X-XSRF-TOKEN", TOKEN_CSRF));
	}

	private String estadoCaja(Long idCaja) {
		return jdbc.queryForObject("SELECT estado FROM CAJA WHERE id_caja = ?", String.class, idCaja);
	}

	// ------------------------------------------------------------------ CUS46

	@Test
	void cus46LaAdministradoraApruebaElCierreConDiferenciaYReabreLaCajaDelDia() throws Exception {
		Long idCaja = cajaAbierta();
		cerrarCaja(idCaja, "1").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADA"))
			// [It. 2.1 · nombres] CajaDTO: quién abrió y quién cerró (USUARIO → PERSONA, Duda 25)
			.andExpect(jsonPath("$.nombreUsuarioApertura").value(jdbc.queryForObject("SELECT TRIM(p.nombres || ' ' || p.apellidos) FROM USUARIO u JOIN PERSONA p ON p.id_persona = u.id_personal WHERE u.nombre_usuario = ?", String.class, "recepcion")))
			.andExpect(jsonPath("$.nombreUsuarioCierre").value(jdbc.queryForObject("SELECT TRIM(p.nombres || ' ' || p.apellidos) FROM USUARIO u JOIN PERSONA p ON p.id_persona = u.id_personal WHERE u.nombre_usuario = ?", String.class, "recepcion")));

		// La recepcionista cierra su caja (CAJA:EDITAR) pero no la aprueba ni la reabre
		enviar(get("/api/v1/cajas/con-diferencia"), null).andExpect(status().isForbidden());
		enviar(patch("/api/v1/cajas/" + idCaja + "/revision"), null).andExpect(status().isForbidden());
		enviar(patch("/api/v1/cajas/" + idCaja + "/reapertura"), "{\"motivoReapertura\":\"Venta tardía\"}").andExpect(status().isForbidden());

		como("admin", get("/api/v1/cajas/con-diferencia"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idCaja", hasItem(idCaja.intValue())))
			.andExpect(jsonPath("$[?(@.idCaja == " + idCaja + ")].arqueos[0].observacion", hasItem("Diferencia de prueba")));
		como("admin", get("/api/v1/avisos"), null).andExpect(status().isOk()).andExpect(jsonPath("$[*].tipo", hasItem("CIERRE_POR_APROBAR")));

		// RF67: queda revisada y sigue CERRADA; no se aprueba dos veces
		como("admin", patch("/api/v1/cajas/" + idCaja + "/revision"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CERRADA"))
			.andExpect(jsonPath("$.idUsuarioRevisa").value(id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'admin'")))
			.andExpect(jsonPath("$.fechaRevision").exists())
			.andExpect(jsonPath("$.nombreUsuarioRevisa").value(jdbc.queryForObject("SELECT TRIM(p.nombres || ' ' || p.apellidos) FROM USUARIO u JOIN PERSONA p ON p.id_persona = u.id_personal WHERE u.nombre_usuario = ?", String.class, "admin")));
		como("admin", patch("/api/v1/cajas/" + idCaja + "/revision"), null).andExpect(status().isUnprocessableContent());
		como("admin", get("/api/v1/cajas/con-diferencia"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idCaja", not(hasItem(idCaja.intValue()))));

		// RF66, RN30: sin motivo 422; con motivo vuelve a ABIERTA y Oracle limpia el cierre y la revisión
		como("admin", get("/api/v1/cajas/del-dia"), null).andExpect(status().isOk()).andExpect(jsonPath("$[*].idCaja", hasItem(idCaja.intValue())));
		como("admin", patch("/api/v1/cajas/" + idCaja + "/reapertura"), "{}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN30"));
		como("admin", patch("/api/v1/cajas/" + idCaja + "/reapertura"), "{\"motivoReapertura\":\"Venta registrada después del cierre\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("ABIERTA"))
			.andExpect(jsonPath("$.motivoReapertura").value("Venta registrada después del cierre"))
			.andExpect(jsonPath("$.idUsuarioReapertura").isNumber())
			.andExpect(jsonPath("$.nombreUsuarioReapertura").value(jdbc.queryForObject("SELECT TRIM(p.nombres || ' ' || p.apellidos) FROM USUARIO u JOIN PERSONA p ON p.id_persona = u.id_personal WHERE u.nombre_usuario = ?", String.class, "admin")))
			.andExpect(jsonPath("$.fechaRevision").value(nullValue()))
			.andExpect(jsonPath("$.fechaCierre").value(nullValue()));
		// [It. 2.1 · D-15] La reapertura queda en la auditoría: quién la hizo y su motivo (TRG_AUD_CAJA de v5.2)
		assertThat(id("SELECT COUNT(*) FROM V_AUDITORIA WHERE entidad = 'CAJA' AND id_entidad = ? AND usuario = 'admin'"
				+ " AND detalle LIKE '%motivo_reapertura:%Venta registrada después del cierre%'"
				+ " AND detalle LIKE '%id_usuario_reapertura:%' AND detalle LIKE '%estado: CERRADA -> ABIERTA%'", idCaja)).isEqualTo(1);

		// La recepcionista vuelve a arquear y cerrar (CUS45, sin cambios)
		cerrarCaja(idCaja, "100").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADA"));
	}

	@Test
	void cus46LaCajaDeAyerNoSeReabreNiEnJavaNiEnOracle() throws Exception {
		Long idCaja = cajaAbierta();
		cerrarCaja(idCaja, "100").andExpect(status().isOk());
		// La caja pasa a ser de ayer (el dueño, en mantenimiento; TRG_CAJA_REGLAS no protege la fecha de apertura de una caja cerrada)
		comoDueno("UPDATE CAJA SET fecha_apertura = fecha_apertura - INTERVAL '1' DAY WHERE id_caja = " + idCaja);

		// Java: Caja.reabrir responde 422 (RN30)
		como("admin", patch("/api/v1/cajas/" + idCaja + "/reapertura"), "{\"motivoReapertura\":\"Venta de ayer\"}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN30"));

		// [It. 2.1 · D-16] Oracle también: aunque se fuerce por SQL con la sesión de la Administradora, TRG_CAJA_REGLAS responde -20060
		Long idAdmin = id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'admin'");
		try (Connection dueno = DriverManager.getConnection(ORACLE.getJdbcUrl(), DUENO, CLAVE_DUENO); Statement sentencia = dueno.createStatement()) {
			sentencia.execute("BEGIN PKG_SESION.iniciar(" + idAdmin + "); END;");
			assertThatThrownBy(() -> sentencia.executeUpdate("UPDATE CAJA SET estado = 'ABIERTA', motivo_reapertura = 'Venta de ayer' WHERE id_caja = " + idCaja))
				.isInstanceOf(SQLException.class)
				.hasMessageContaining("ORA-20060")
				.hasMessageContaining("Solo se reabre la caja del d");
			sentencia.execute("BEGIN PKG_SESION.terminar; END;");
		}
		assertThat(estadoCaja(idCaja)).isEqualTo("CERRADA");
	}

	@Test
	void cus46ReabrirConOtraCajaAbiertaEnLaSedeEs409() throws Exception {
		Long cerrada = cajaAbierta();
		cerrarCaja(cerrada, "100").andExpect(status().isOk());
		Long abierta = cajaAbierta();

		como("admin", patch("/api/v1/cajas/" + cerrada + "/reapertura"), "{\"motivoReapertura\":\"Venta tardía\"}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RN30"));

		assertThat(estadoCaja(cerrada)).isEqualTo("CERRADA");
		cerrarCaja(abierta, "100").andExpect(status().isOk());
	}

	// ------------------------------------------------------------------ CUS47

	@Test
	void cus47ImportaLaLiquidacionConciliaJustificaYCierraElMes() throws Exception {
		Long idAdmin = id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'admin'");
		// El cobro con tarjeta del reporte de ejemplo, APROBADO y aún sin liquidar
		comoDueno("INSERT INTO TRANSACCION_PASARELA (id_sede, id_medio_pago, tipo_operacion, canal, codigo_operacion, monto, estado, id_usuario, fecha_hora,"
				+ " fecha_respuesta) VALUES (" + sedeDeLaClase() + ", " + medio("TARJETA") + ", 'COBRO', 'POS', 'CS-EJEMPLO-0001', 50, 'APROBADA', " + idAdmin
				+ ", SYSTIMESTAMP, SYSTIMESTAMP)");
		Long idCobro = id("SELECT id_transaccion FROM TRANSACCION_PASARELA WHERE codigo_operacion = 'CS-EJEMPLO-0001'");
		byte[] reporte;
		try (InputStream ejemplo = getClass().getResourceAsStream("/liquidacion/reporte-liquidacion-ejemplo.csv")) {
			reporte = ejemplo.readAllBytes();
		}
		MockMultipartFile archivo = new MockMultipartFile("archivoLiquidacion", "reporte-liquidacion-ejemplo.csv", "text/csv", reporte);

		importar("recepcion", archivo).andExpect(status().isForbidden());
		importar("admin", archivo).andExpect(status().isOk())
			.andExpect(jsonPath("$.filasLeidas").value(2))
			.andExpect(jsonPath("$.filasRegistradas").value(1))
			.andExpect(jsonPath("$.codigosSinTransaccion[0]").value("CS-EJEMPLO-9999"));
		assertThat(jdbc.queryForObject("SELECT comision FROM TRANSACCION_PASARELA WHERE id_transaccion = ?", BigDecimal.class, idCobro))
			.isEqualByComparingTo("1.75");
		assertThat(jdbc.queryForObject("SELECT fecha_abono FROM TRANSACCION_PASARELA WHERE id_transaccion = ?", LocalDate.class, idCobro))
			.isEqualTo(LocalDate.of(2026, 11, 7));

		// Una por mes; el cobro sin venta ni cita queda SIN_JUSTIFICAR
		LocalDate hoy = LocalDate.now();
		String mes = "{\"anio\":" + hoy.getYear() + ",\"mes\":" + hoy.getMonthValue() + "}";
		ResultActions creada = como("admin", post("/api/v1/conciliaciones"), mes).andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("ABIERTA"))
			.andExpect(jsonPath("$.items[?(@.idTransaccion == " + idCobro + ")].estado", hasItem("SIN_JUSTIFICAR")));
		Long idConciliacion = idDe(creada, "idConciliacion");
		como("admin", post("/api/v1/conciliaciones"), mes).andExpect(status().isConflict()).andExpect(jsonPath("$.regla").value("RF75"));

		como("admin", patch("/api/v1/conciliaciones/" + idConciliacion + "/cierre"), null).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF75"));

		// Cada diferencia se justifica (la del cobro, con su texto) o se excluye; sin justificación es 422
		List<Map<String, Object>> items = JsonPath.read(creada.andReturn().getResponse().getContentAsString(), "$.items[?(@.estado == 'SIN_JUSTIFICAR')]");
		Long primero = ((Number) items.getFirst().get("idItem")).longValue();
		como("admin", patch("/api/v1/conciliaciones/" + idConciliacion + "/items/" + primero), "{\"estado\":\"JUSTIFICADA\"}")
			.andExpect(status().isUnprocessableContent());
		for (Map<String, Object> item : items) {
			boolean esElCobro = idCobro.equals(item.get("idTransaccion") == null ? null : ((Number) item.get("idTransaccion")).longValue());
			String cuerpo = esElCobro ? "{\"estado\":\"JUSTIFICADA\",\"justificacion\":\"Cobro de prueba sin venta, liquidado por Izipay\"}"
					: "{\"estado\":\"EXCLUIDA\",\"justificacion\":\"Dato de otra prueba\"}";
			como("admin", patch("/api/v1/conciliaciones/" + idConciliacion + "/items/" + item.get("idItem")), cuerpo).andExpect(status().isOk());
		}

		como("admin", patch("/api/v1/conciliaciones/" + idConciliacion + "/cierre"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CERRADA"))
			.andExpect(jsonPath("$.fechaCierre").exists())
			.andExpect(jsonPath("$.sinJustificar").value(0))
			.andExpect(jsonPath("$.items[?(@.idTransaccion == " + idCobro + ")].estado", hasItem("JUSTIFICADA")));
		// CERRADA ya no cambia
		como("admin", patch("/api/v1/conciliaciones/" + idConciliacion + "/items/" + primero), "{\"estado\":\"EXCLUIDA\"}")
			.andExpect(status().isUnprocessableContent());
	}

	// ------------------------------------------------------------------ CUS54 y CUS55

	@Test
	void cus54RestableceLaClaveDesbloqueaLaCuentaYCus55LaAuditoria() throws Exception {
		Long idRol = id("SELECT id_rol FROM ROL WHERE nombre = 'ESPECIALISTA_TECNICA'");
		String nombreUsuario = "bloqueada" + sedeDeLaClase();
		Long idUsuario = idDe(como("admin", post("/api/v1/usuarios"), "{\"idPersonal\":" + especialista() + ",\"idRol\":" + idRol + ",\"nombreUsuario\":\""
				+ nombreUsuario + "\",\"clave\":\"ClaveInicial2026\"}").andExpect(status().isCreated()), "idUsuario");
		comoDueno("UPDATE USUARIO SET intentos_fallidos = 5, estado = 'BLOQUEADO', bloqueado_hasta = SYSTIMESTAMP + INTERVAL '30' MINUTE"
				+ " WHERE id_usuario = " + idUsuario);

		enviar(patch("/api/v1/usuarios/" + idUsuario + "/clave"), "{\"clave\":\"Temporal2026\"}").andExpect(status().isForbidden());
		como("admin", patch("/api/v1/usuarios/" + idUsuario + "/clave"), "{\"clave\":\"corta\"}").andExpect(status().isBadRequest());
		como("admin", patch("/api/v1/usuarios/" + idUsuario + "/clave"), "{\"clave\":\"Temporal2026\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.debeCambiarClave").value(true))
			.andExpect(jsonPath("$.estado").value("ACTIVO"))
			.andExpect(jsonPath("$.clave").doesNotExist());

		Map<String, Object> fila = jdbc.queryForMap("SELECT clave_hash, debe_cambiar_clave, intentos_fallidos, estado FROM USUARIO WHERE id_usuario = ?",
				idUsuario);
		assertThat((String) fila.get("CLAVE_HASH")).startsWith("$2a$10$");
		assertThat(new BCryptPasswordEncoder().matches("Temporal2026", (String) fila.get("CLAVE_HASH"))).isTrue();
		assertThat(fila.get("DEBE_CAMBIAR_CLAVE")).isEqualTo("S");
		assertThat(((Number) fila.get("INTENTOS_FALLIDOS")).intValue()).isZero();
		assertThat(fila.get("ESTADO")).isEqualTo("ACTIVO");

		// CUS55: lo auditaron los triggers; los filtros son opcionales y un rango invertido es 422
		String hoy = LocalDate.now().toString();
		como("admin", get("/api/v1/auditoria").param("entidad", "USUARIO").param("fechaInicio", hoy).param("fechaFin", hoy), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idEntidad", hasItem(idUsuario.intValue())))
			.andExpect(jsonPath("$[*].entidad", everyItem(org.hamcrest.Matchers.is("USUARIO"))));
		como("admin", get("/api/v1/auditoria").param("fechaInicio", hoy).param("fechaFin", LocalDate.now().minusDays(1).toString()), null)
			.andExpect(status().isUnprocessableContent());
		enviar(get("/api/v1/auditoria"), null).andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ CUS62 y CUS63

	@Test
	void cus62ElEfectivoNoUsaLaPasarelaYCus63NoRepiteLaSerie() throws Exception {
		Long efectivo = medio("EFECTIVO");
		enviar(get("/api/v1/medios-pago"), null).andExpect(status().isOk()).andExpect(jsonPath("$[*].nombre", hasItem("EFECTIVO")));
		como("admin", patch("/api/v1/medios-pago/" + efectivo), "{\"usaPasarela\":true}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN42"));
		como("admin", patch("/api/v1/medios-pago/" + medio("YAPE")), "{\"estado\":\"ACTIVO\",\"usaPasarela\":true}").andExpect(status().isOk())
			.andExpect(jsonPath("$.usaPasarela").value(true));
		enviar(patch("/api/v1/medios-pago/" + medio("YAPE")), "{\"estado\":\"INACTIVO\"}").andExpect(status().isForbidden());

		Long idSede = sedeDeLaClase();
		String serie = String.format("BX%02d", idSede % 100);
		como("admin", post("/api/v1/series-comprobante"), "{\"idSede\":" + idSede + ",\"tipo\":\"NC_BOLETA\",\"serie\":\"" + serie + "\"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("ACTIVA"))
			.andExpect(jsonPath("$.ultimoNumero").value(0));
		como("admin", get("/api/v1/series-comprobante").param("idSede", idSede.toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].serie", hasItem(serie)));
		// UQ_SERIE_COMPR_ACTIVA: otra NC_BOLETA ACTIVA en la sede; y la misma serie en otra sede
		como("admin", post("/api/v1/series-comprobante"), "{\"idSede\":" + idSede + ",\"tipo\":\"NC_BOLETA\",\"serie\":\"" + String.format("BY%02d", idSede % 100)
				+ "\"}").andExpect(status().isConflict());
		como("admin", post("/api/v1/series-comprobante"), "{\"idSede\":" + id("SELECT MIN(id_sede) FROM SEDE") + ",\"tipo\":\"NC_BOLETA\",\"serie\":\"" + serie
				+ "\"}").andExpect(status().isConflict());
		como("admin", post("/api/v1/series-comprobante"), "{\"idSede\":" + idSede + ",\"tipo\":\"FACTURA\",\"serie\":\"B999\"}")
			.andExpect(status().isUnprocessableContent());
	}

	// ------------------------------------------------------------------ CUS65

	@Test
	void cus65CambiaParametrosConSuRangoYConfiguraTiposDeDocumento() throws Exception {
		// [It. 2.1 · v5.2] LIMPIEZA_MAX_MIN se retiró con el tiempo de limpieza (RN03): ya no se lista y su PATCH es 404, como cualquier código
		como("admin", get("/api/v1/parametros"), null).andExpect(status().isOk()).andExpect(jsonPath("$[*].codigo", hasItem("AGENDA_PASO_MIN")))
			.andExpect(jsonPath("$[*].codigo", not(hasItem("LIMPIEZA_MAX_MIN"))));
		BigDecimal paso = jdbc.queryForObject("SELECT valor_numero FROM PARAMETRO WHERE codigo = 'AGENDA_PASO_MIN'", BigDecimal.class);

		como("admin", patch("/api/v1/parametros/LIMPIEZA_MAX_MIN"), "{\"valorNumero\":10}").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.regla").value("RF87"));
		como("admin", patch("/api/v1/parametros/AGENDA_PASO_MIN"), "{\"valorNumero\":0}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN51"));
		como("admin", patch("/api/v1/parametros/NO_EXISTE"), "{\"valorNumero\":1}").andExpect(status().isNotFound());
		enviar(patch("/api/v1/parametros/AGENDA_PASO_MIN"), "{\"valorNumero\":5}").andExpect(status().isForbidden());
		// El mismo valor: queda auditado a nombre de la Administradora y no cambia nada para las demás pruebas
		como("admin", patch("/api/v1/parametros/AGENDA_PASO_MIN"), "{\"valorNumero\":" + paso.toPlainString() + "}").andExpect(status().isOk())
			.andExpect(jsonPath("$.idUsuario").value(id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'admin'")));
		assertThat(jdbc.queryForObject("SELECT valor_numero FROM PARAMETRO WHERE codigo = 'AGENDA_PASO_MIN'", BigDecimal.class)).isEqualByComparingTo(paso);

		// GET /tipos-documento lo lee cualquier usuario con sesión; solo DNI y RUC se consultan en APISPERU (RN45)
		como("especialista1", get("/api/v1/tipos-documento"), null).andExpect(status().isOk()).andExpect(jsonPath("$[*].abreviatura", hasItem("DNI")));
		Long pasaporte = id("SELECT MIN(id_tipo_documento) FROM TIPO_DOCUMENTO WHERE consulta_api = 'N'");
		como("admin", patch("/api/v1/tipos-documento/" + pasaporte), "{\"consultaApi\":true}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN45"));
		como("admin", patch("/api/v1/tipos-documento/" + pasaporte), "{\"consultaApi\":false,\"estado\":\"ACTIVO\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("ACTIVO"));
	}

}
