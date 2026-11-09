package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.jayway.jsonpath.JsonPath;
import dev.com.Synapsis_Spa_Cautiva.comun.api.AuditoriaApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ParametroApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.UsuarioSesion;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Flujos de CUS50 a CUS53, CUS57 y CUS61 con los usuarios del script 15. Los datos se confirman, como en producción, y la
 * base es compartida: admin y recepcion cambian su clave una sola vez, y otros usuarios cubren el bloqueo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadCatalogoFlujoTest extends OracleTestcontainer {

	private static final String CLAVE_TEMPORAL = "CambiarAlEntrar2026";
	private static final String CLAVE_NUEVA = "ClaveNueva2026";
	private static final int INTENTOS_MAX = 5;

	private static Sesion sesionAdmin;

	private static Sesion sesionRecepcion;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PersonaApi personaApi;

	@Autowired
	private AuditoriaApi auditoriaApi;

	@Autowired
	private ParametroApi parametroApi;

	private record Sesion(MockHttpSession http, Cookie xsrf) {
	}

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	// ------------------------------------------------------------------ apoyo

	private ResultActions login(String usuario, String clave) throws Exception {
		return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"nombreUsuario\":\"" + usuario + "\",\"clave\":\"" + clave + "\"}"));
	}

	private Sesion ingresar(String usuario, String clave) throws Exception {
		MvcResult resultado = login(usuario, clave).andExpect(status().isOk()).andReturn();
		return new Sesion((MockHttpSession) resultado.getRequest().getSession(false), resultado.getResponse().getCookie("XSRF-TOKEN"));
	}

	private ResultActions enviar(Sesion sesion, MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		pedido.session(sesion.http()).cookie(sesion.xsrf()).header("X-XSRF-TOKEN", sesion.xsrf().getValue());
		if (cuerpo != null) {
			pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
		}
		return mvc.perform(pedido);
	}

	private void cambiarClave(Sesion sesion, String claveActual, String claveNueva) throws Exception {
		enviar(sesion, post("/api/v1/auth/cambio-clave"),
				"{\"claveActual\":\"" + claveActual + "\",\"claveNueva\":\"" + claveNueva + "\"}").andExpect(status().isOk());
	}

	private Sesion admin() throws Exception {
		if (sesionAdmin == null) {
			sesionAdmin = ingresar("admin", CLAVE_TEMPORAL);
			cambiarClave(sesionAdmin, CLAVE_TEMPORAL, CLAVE_NUEVA);
		}
		return sesionAdmin;
	}

	private Sesion recepcion() throws Exception {
		if (sesionRecepcion == null) {
			sesionRecepcion = ingresar("recepcion", CLAVE_TEMPORAL);
			cambiarClave(sesionRecepcion, CLAVE_TEMPORAL, CLAVE_NUEVA);
		}
		return sesionRecepcion;
	}

	private Long id(String sql) {
		return jdbc.queryForObject(sql, Long.class);
	}

	private Long idUsuario(String nombre) {
		return id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = '" + nombre + "'");
	}

	private Long idSede() {
		return id("SELECT MIN(id_sede) FROM SEDE");
	}

	private void comoAdministradora() {
		UsuarioSesion usuario = new UsuarioSesion(idUsuario("admin"), "admin", idSede(), false, Set.of());
		SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of()));
	}

	private void comoDueno(String sql) throws SQLException {
		try (Connection dueno = DriverManager.getConnection(ORACLE.getJdbcUrl(), DUENO, CLAVE_DUENO);
				Statement sentencia = dueno.createStatement()) {
			sentencia.executeUpdate(sql);
		}
	}

	// ------------------------------------------------------------------ CUS50 y cambio de clave

	@Test
	void cus50ElIngresoEntregaPermisosSedeYVenceSegunElParametro() throws Exception {
		MvcResult resultado = login("especialista1", CLAVE_TEMPORAL).andExpect(status().isOk())
			.andExpect(jsonPath("$.nombreUsuario").value("especialista1"))
			.andExpect(jsonPath("$.idSede").value(idSede()))
			.andExpect(jsonPath("$.debeCambiarClave").value(true))
			.andExpect(jsonPath("$.permisos", hasItem("ATENCION:VER")))
			.andReturn();

		int minutos = parametroApi.valorNumero("SESION_INACTIVIDAD_MIN").intValue();
		assertThat(resultado.getRequest().getSession(false).getMaxInactiveInterval()).isEqualTo(minutos * 60);
		assertThat(resultado.getResponse().getCookie("XSRF-TOKEN")).isNotNull();
		assertThat(id("SELECT COUNT(*) FROM USUARIO WHERE nombre_usuario = 'especialista1' AND ultimo_acceso IS NOT NULL")).isEqualTo(1);
	}

	@Test
	void cus50LaClaveErradaSoloSumaIntentosYOracleBloquea() throws Exception {
		Long idUsuario = idUsuario("especialista2");
		for (int intento = 1; intento <= INTENTOS_MAX; intento++) {
			login("especialista2", "ClaveErrada").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.regla").value("RF77"));
		}
		assertThat(jdbc.queryForObject("SELECT estado FROM USUARIO WHERE id_usuario = ?", String.class, idUsuario)).isEqualTo("BLOQUEADO");
		assertThat(id("SELECT COUNT(*) FROM AUDITORIA WHERE accion = 'INICIO_SESION_FALLIDO' AND id_usuario = " + idUsuario)).isEqualTo(INTENTOS_MAX);

		login("especialista2", CLAVE_TEMPORAL).andExpect(status().isLocked()).andExpect(jsonPath("$.detail").exists());

		// Vencido el bloqueo, la clave correcta entra y los intentos vuelven a 0
		comoDueno("UPDATE USUARIO SET bloqueado_hasta = SYSTIMESTAMP - INTERVAL '1' MINUTE WHERE id_usuario = " + idUsuario);
		login("especialista2", CLAVE_TEMPORAL).andExpect(status().isOk());
		assertThat(id("SELECT intentos_fallidos FROM USUARIO WHERE id_usuario = " + idUsuario)).isZero();
		assertThat(jdbc.queryForObject("SELECT estado FROM USUARIO WHERE id_usuario = ?", String.class, idUsuario)).isEqualTo("ACTIVO");
	}

	@Test
	void cus50UnaCuentaInactivaRecibe423YUnUsuarioDesconocido401() throws Exception {
		comoDueno("UPDATE USUARIO SET estado = 'INACTIVO' WHERE nombre_usuario = 'especialista1'");
		try {
			login("especialista1", CLAVE_TEMPORAL).andExpect(status().isLocked()).andExpect(jsonPath("$.regla").value("RF53"));
		}
		finally {
			comoDueno("UPDATE USUARIO SET estado = 'ACTIVO' WHERE nombre_usuario = 'especialista1'");
		}
		login("no-existe", "cualquiera").andExpect(status().isUnauthorized());
	}

	@Test
	void conClaveTemporalSoloSePuedeCambiarLaClave() throws Exception {
		Sesion temporal = ingresar("especialista1", CLAVE_TEMPORAL);
		enviar(temporal, get("/api/v1/areas"), null).andExpect(status().isForbidden()).andExpect(jsonPath("$.regla").value("RF82"));
		enviar(temporal, post("/api/v1/auth/cambio-clave"), "{\"claveActual\":\"Otra12345\",\"claveNueva\":\"" + CLAVE_NUEVA + "\"}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF82"));
		enviar(temporal, post("/api/v1/auth/cambio-clave"), "{\"claveActual\":\"" + CLAVE_TEMPORAL + "\",\"claveNueva\":\"corta\"}")
			.andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------ CUS51 y sesión

	@Test
	void cus51CerrarSesionAuditaEInvalidaLaSesion() throws Exception {
		Sesion sesion = ingresar("especialista1", CLAVE_TEMPORAL);
		Long idUsuario = idUsuario("especialista1");
		Long antes = id("SELECT COUNT(*) FROM AUDITORIA WHERE accion = 'CERRAR_SESION' AND id_usuario = " + idUsuario);

		enviar(sesion, post("/api/v1/auth/logout"), null).andExpect(status().isOk());

		assertThat(id("SELECT COUNT(*) FROM AUDITORIA WHERE accion = 'CERRAR_SESION' AND id_usuario = " + idUsuario)).isEqualTo(antes + 1);
		assertThat(sesion.http().isInvalid()).isTrue();
	}

	@Test
	void sinSesionEs401YSinTokenCsrfEs403() throws Exception {
		mvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.regla").value("SIN_SESION"));

		Sesion sesion = admin();
		mvc.perform(post("/api/v1/areas").session(sesion.http()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"linea\":\"SPA\",\"nombreArea\":\"Sin token\"}")).andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ CUS52 y CUS53

	@Test
	void cus52AdminCambiaLaClaveYRegistraALaRecepcionistaDos() throws Exception {
		Sesion sesion = admin();
		enviar(sesion, get("/api/v1/roles"), null).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));

		Long idPersonal = id("SELECT id_persona FROM PERSONA WHERE numero_documento = '70000003'");
		Long idRol = id("SELECT id_rol FROM ROL WHERE nombre = 'RECEPCIONISTA'");
		String cuerpo = "{\"idPersonal\":" + idPersonal + ",\"idRol\":" + idRol + ",\"nombreUsuario\":\"recepcion2\",\"clave\":\"" + CLAVE_TEMPORAL + "\"}";

		enviar(sesion, post("/api/v1/usuarios"), cuerpo).andExpect(status().isCreated())
			.andExpect(header().exists("Location"))
			.andExpect(jsonPath("$.debeCambiarClave").value(true))
			.andExpect(jsonPath("$.estado").value("ACTIVO"))
			.andExpect(jsonPath("$.clave").doesNotExist());
		assertThat(jdbc.queryForObject("SELECT clave_hash FROM USUARIO WHERE nombre_usuario = 'recepcion2'", String.class)).startsWith("$2a$10$");
		login("recepcion2", CLAVE_TEMPORAL).andExpect(status().isOk());

		// Errores de regla: nombre repetido, persona con usuario, persona que no es del personal
		enviar(sesion, post("/api/v1/usuarios"), cuerpo).andExpect(status().isConflict()).andExpect(jsonPath("$.regla").value("RF81"));
		enviar(sesion, post("/api/v1/usuarios"), cuerpo.replace("recepcion2", "recepcion3")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.constraint").value("UQ_USUARIO_ID_PERSONAL"));
		enviar(sesion, post("/api/v1/usuarios"), cuerpo.replace("recepcion2", "recepcion3").replace(idPersonal.toString(), "999999"))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void cus52RecepcionRecibe403YQuedaAuditado() throws Exception {
		Sesion sesion = recepcion();
		Long idRecepcion = idUsuario("recepcion");
		Long antes = id("SELECT COUNT(*) FROM AUDITORIA WHERE accion = 'ACCESO_DENEGADO' AND id_usuario = " + idRecepcion);

		enviar(sesion, post("/api/v1/usuarios"), "{\"idPersonal\":1,\"idRol\":1,\"nombreUsuario\":\"intruso\",\"clave\":\"" + CLAVE_NUEVA + "\"}")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.regla").value("ACCESO_DENEGADO"));

		assertThat(id("SELECT COUNT(*) FROM AUDITORIA WHERE accion = 'ACCESO_DENEGADO' AND id_usuario = " + idRecepcion)).isEqualTo(antes + 1);
		assertThat(id("SELECT COUNT(*) FROM USUARIO WHERE nombre_usuario = 'intruso'")).isZero();
		// [v5.1] RECEPCIONISTA tiene CATALOGO:VER (script 17), pero no CATALOGO:CREAR
		enviar(sesion, get("/api/v1/areas"), null).andExpect(status().isOk());
		enviar(sesion, post("/api/v1/areas"), "{\"linea\":\"SALON\",\"nombreArea\":\"Intrusa\"}").andExpect(status().isForbidden());
		enviar(sesion, post("/api/v1/recursos"), "{\"idSede\":1,\"nombre\":\"Intruso\",\"tipo\":\"CABINA\",\"minutosLimpieza\":5}")
			.andExpect(status().isForbidden());
	}

	@Test
	void cus53AsignaRolYReemplazaLosPermisos() throws Exception {
		Sesion sesion = admin();
		Long idUsuario = idUsuario("especialista1");
		Long rolOriginal = id("SELECT id_rol FROM USUARIO WHERE id_usuario = " + idUsuario);
		Long idAlmacen = id("SELECT id_rol FROM ROL WHERE nombre = 'ENCARGADA_ALMACEN'");

		enviar(sesion, patch("/api/v1/usuarios/" + idUsuario + "/rol"), "{\"idRol\":" + idAlmacen + "}").andExpect(status().isOk())
			.andExpect(jsonPath("$.idRol").value(idAlmacen));
		enviar(sesion, patch("/api/v1/usuarios/" + idUsuario + "/rol"), "{\"idRol\":" + rolOriginal + "}").andExpect(status().isOk());
		enviar(sesion, patch("/api/v1/usuarios/999999/rol"), "{\"idRol\":" + idAlmacen + "}").andExpect(status().isNotFound());
		enviar(sesion, patch("/api/v1/usuarios/" + idUsuario + "/rol"), "{\"idRol\":999999}").andExpect(status().isUnprocessableContent());

		String permisos = "{\"descripcion\":\"Almacén y kardex\",\"estado\":\"ACTIVO\",\"permisos\":["
				+ "{\"modulo\":\"PRODUCTOS\",\"accion\":\"VER\"},{\"modulo\":\"INVENTARIO_PRUEBA\",\"accion\":\"VER\"}]}";
		enviar(sesion, put("/api/v1/roles/" + idAlmacen + "/permisos"), permisos).andExpect(status().isOk())
			.andExpect(jsonPath("$.descripcion").value("Almacén y kardex"))
			.andExpect(jsonPath("$.permisos", hasSize(2)))
			.andExpect(jsonPath("$.permisos[*].modulo", hasItem("INVENTARIO_PRUEBA")))
			.andExpect(jsonPath("$.permisos[*].modulo", not(hasItem("KARDEX"))));
		enviar(sesion, put("/api/v1/roles/999999/permisos"), permisos).andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------------ CUS57

	@Test
	void cus57RegistraAreaServicioVarianteEInsumo() throws Exception {
		Sesion sesion = admin();
		enviar(sesion, get("/api/v1/areas"), null).andExpect(status().isOk()).andExpect(jsonPath("$[*].nombreArea", hasItem("Cuidado Facial")));

		String area = "{\"linea\":\"SPA\",\"nombreArea\":\"Masajes de prueba\"}";
		Number idArea = JsonPath.read(enviar(sesion, post("/api/v1/areas"), area).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString(), "$.idArea");
		enviar(sesion, post("/api/v1/areas"), area).andExpect(status().isConflict()).andExpect(jsonPath("$.constraint").value("UQ_AREA_NOMBRE"));

		Long idProducto = id("SELECT MIN(id_producto) FROM PRODUCTO");
		String servicio = "{\"idArea\":" + idArea + ",\"nombre\":\"Masaje relajante\",\"duracionRefMin\":60,\"tipoRecurso\":\"CAMILLA\",\"variantes\":["
				+ "{\"nombreVariante\":\"Corto\",\"precio\":50.00,\"insumos\":[{\"idProducto\":" + idProducto + ",\"cantidadEstimada\":1.5}]},"
				+ "{\"nombreVariante\":\"Largo\",\"precio\":90.00}]}";
		enviar(sesion, post("/api/v1/servicios"), servicio).andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("ACTIVO"))
			.andExpect(jsonPath("$.variantes", hasSize(2)))
			.andExpect(jsonPath("$.variantes[0].nombreVariante").value("Corto"))
			.andExpect(jsonPath("$.variantes[0].insumos[0].idProducto").value(idProducto));

		enviar(sesion, post("/api/v1/servicios"), servicio.replace("Masaje relajante", "Con precio negativo").replace("50.00", "-1"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN40"));
		enviar(sesion, post("/api/v1/servicios"), servicio).andExpect(status().isConflict())
			.andExpect(jsonPath("$.constraint").value("UQ_SERVICIO_ID_AREA_NOMBRE"));
	}

	// ------------------------------------------------------------------ CUS61 y sesión Oracle

	@Test
	void cus61RegistraRecursoConLaLimpiezaDelParametro() throws Exception {
		Sesion sesion = admin();
		// LIMPIEZA_MAX_MIN = 10 desde v5.1 (RN03)
		String recurso = "{\"idSede\":" + idSede() + ",\"nombre\":\"Camilla de prueba\",\"tipo\":\"CAMILLA\",\"minutosLimpieza\":10}";
		enviar(sesion, post("/api/v1/recursos"), recurso).andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("DISPONIBLE"));
		enviar(sesion, post("/api/v1/recursos"), recurso).andExpect(status().isConflict());
		enviar(sesion, post("/api/v1/recursos"), recurso.replace("Camilla de prueba", "Otra").replace("\"minutosLimpieza\":10", "\"minutosLimpieza\":11"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN03"));
	}

	@Test
	void sinSesionOracleLaOperacionFallaConMenos20001() {
		assertThatThrownBy(() -> jdbc.update("UPDATE ROL SET descripcion = descripcion || 'x' WHERE nombre = 'RECEPCIONISTA'"))
			.hasMessageContaining("ORA-20001");
	}

	@Test
	void conSesionOracleLaAuditoriaLlevaElIdDelUsuario() throws Exception {
		Sesion sesion = admin();
		String recurso = "{\"idSede\":" + idSede() + ",\"nombre\":\"Recurso auditado\",\"tipo\":\"CABINA\",\"minutosLimpieza\":10}";
		Number idRecurso = JsonPath.read(enviar(sesion, post("/api/v1/recursos"), recurso).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString(), "$.idRecurso");

		Long idAuditor = id("SELECT id_usuario FROM AUDITORIA WHERE entidad = 'RECURSO' AND accion = 'CREAR' AND id_entidad = " + idRecurso);
		assertThat(idAuditor).isEqualTo(idUsuario("admin"));
		assertThat(jdbc.queryForObject("SELECT SYS_CONTEXT('USERENV', 'CLIENT_IDENTIFIER') FROM dual", String.class)).isNull();
	}

	@Test
	void unUsuarioQueOracleRechazaNoDejaConexionesTomadas() {
		UsuarioSesion inexistente = new UsuarioSesion(999999L, "fantasma", 1L, false, Set.of());
		SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(inexistente, null, List.of()));
		for (int vez = 0; vez < 15; vez++) {
			assertThatThrownBy(() -> parametroApi.valorNumero("IGV_PORCENTAJE")).hasStackTraceContaining("ORA-20004");
		}
		comoAdministradora();
		assertThat(parametroApi.valorNumero("IGV_PORCENTAJE")).isNotNull();
	}

	// ------------------------------------------------------------------ PersonaApi y AuditoriaApi

	private PersonaDTO persona(Long idTipoDocumento, String numero, String telefono) {
		return new PersonaDTO(null, null, "Ana", "Prueba", null, idTipoDocumento, numero, telefono, null, null, null);
	}

	@Test
	void personaApiDejaLaFichaManualSiLaConsultaFalla() {
		comoAdministradora();
		Long idDni = id("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'");
		Long idPasaporte = id("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE consulta_api = 'N' FETCH FIRST 1 ROW ONLY");

		assertThat(personaApi.consultarDocumento(idDni, "41000001").resultado()).isEqualTo("ERROR");
		assertThatThrownBy(() -> personaApi.consultarDocumento(idDni, "123")).isInstanceOf(ReglaNegocioException.class).hasMessageContaining("formato");
		assertThatThrownBy(() -> personaApi.consultarDocumento(idPasaporte, "ABC123456")).isInstanceOf(ReglaNegocioException.class);

		Long idPersona = personaApi.registrarPersona(persona(idDni, "41000001", "987654000"));
		assertThat(personaApi.obtenerPersona(idPersona).origenDatos()).isEqualTo("MANUAL");
		assertThat(personaApi.registrarPersona(persona(idDni, "41000001", "987654000"))).isEqualTo(idPersona);
		assertThat(personaApi.buscarPersonas(null, "987654000")).extracting(PersonaDTO::idPersona).containsExactly(idPersona);

		Long otra = personaApi.registrarPersona(persona(null, null, "987654001"));
		assertThatThrownBy(() -> personaApi.registrarPersona(persona(idDni, "41000001", "987654001"))).isInstanceOf(ReglaNegocioException.class)
			.hasMessageContaining("fichas distintas");

		personaApi.actualizarPersona(otra, new PersonaDTO(null, null, "Ana María", "Prueba", null, null, null, "987654001", "ana@correo.pe", null, null));
		assertThat(personaApi.obtenerPersona(otra).nombres()).isEqualTo("Ana María");
		assertThat(personaApi.obtenerPersona(otra).correo()).isEqualTo("ana@correo.pe");
		assertThat(id("SELECT COUNT(*) FROM AUDITORIA WHERE entidad = 'PERSONA' AND accion = 'EDITAR' AND id_entidad = " + otra)).isEqualTo(1);
		assertThatThrownBy(() -> personaApi.obtenerPersona(999999L)).isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void auditoriaApiConsultaLaVista() throws Exception {
		Sesion sesion = recepcion();
		enviar(sesion, get("/api/v1/roles"), null).andExpect(status().isForbidden());
		comoAdministradora();

		assertThat(auditoriaApi.consultar(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)))
			.anySatisfy(registro -> {
				assertThat(registro.accion()).isEqualTo("ACCESO_DENEGADO");
				assertThat(registro.usuario()).isEqualTo("recepcion");
				assertThat(registro.rol()).isEqualTo("RECEPCIONISTA");
				assertThat(registro.detalle()).isEqualTo("GET /api/v1/roles");
			});
	}

}
