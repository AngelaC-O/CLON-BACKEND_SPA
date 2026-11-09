package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionCobrable;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioClientePaquete;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.UsuarioSesion;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Flujos de CUS01, CUS02, CUS05, CUS06, CUS08 y CUS11 con el personal y los permisos del script 15. Los datos se confirman y la base es
 * compartida: las citas van a semanas lejanas, cada prueba usa su propio lunes, y las pruebas de otras clases ya usan el próximo lunes.
 * La clave de los usuarios la cambia SeguridadCatalogoFlujoTest, así que aquí la sesión se arma con los permisos reales del rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AgendaFlujoTest extends OracleTestcontainer {

	private static final AtomicInteger TELEFONOS = new AtomicInteger(987_610_000);

	private static final String CORTE = "Corte de Cabello Caballero";

	private static final String TOKEN_CSRF = "token-de-prueba";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private RepositorioCita repositorioCita;

	@Autowired
	private RepositorioClientePaquete repositorioClientePaquete;

	@Autowired
	private AtencionApi atencionApi;

	@Autowired
	private TransactionTemplate transacciones;

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	// ------------------------------------------------------------------ apoyo

	private Long id(String sql, Object... parametros) {
		return jdbc.queryForObject(sql, Long.class, parametros);
	}

	private Long idPersonal(String usuario) {
		return id("SELECT id_personal FROM USUARIO WHERE nombre_usuario = ?", usuario);
	}

	private Long idSede() {
		return id("SELECT MIN(id_sede) FROM SEDE");
	}

	private Long idVariante(String servicio) {
		return id("SELECT v.id_variante FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio WHERE s.nombre = ?", servicio);
	}

	private Long recurso(String tipo, int posicion) {
		List<Long> ids = jdbc.queryForList("SELECT id_recurso FROM RECURSO WHERE tipo = ? ORDER BY id_recurso", Long.class, tipo);
		return ids.get(posicion);
	}

	/** Un lunes propio de cada prueba, lejos del próximo lunes que usan otras clases. */
	private LocalDate lunes(int semana) {
		return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(semana);
	}

	private Authentication sesion(String usuario) {
		Long idUsuario = id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = ?", usuario);
		List<String> permisos = jdbc.queryForList("""
				SELECT p.modulo || ':' || p.accion FROM USUARIO u
				JOIN ROL_PERMISO rp ON rp.id_rol = u.id_rol
				JOIN PERMISO p ON p.id_permiso = rp.id_permiso
				WHERE u.nombre_usuario = ?
				""", String.class, usuario);
		UsuarioSesion principal = new UsuarioSesion(idUsuario, usuario, idSede(), false, new HashSet<>(permisos));
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, permisos.stream().map(SimpleGrantedAuthority::new).toList());
	}

	/** La cookie y la cabecera como las manda Angular: csrf() de spring-security-test cambia el repositorio del contexto compartido. */
	private ResultActions enviar(String usuario, MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		pedido.with(authentication(sesion(usuario))).cookie(new Cookie("XSRF-TOKEN", TOKEN_CSRF)).header("X-XSRF-TOKEN", TOKEN_CSRF);
		if (cuerpo != null) {
			pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
		}
		return mvc.perform(pedido);
	}

	private String telefonoNuevo() {
		return String.valueOf(TELEFONOS.incrementAndGet());
	}

	private String cuerpoClienta(String nombres, String telefono) {
		return "{\"nombres\":\"" + nombres + "\",\"apellidos\":\"De Prueba\",\"telefono\":\"" + telefono + "\",\"aceptaComunicaciones\":true}";
	}

	private Long registrarClienta(String nombres) throws Exception {
		String respuesta = enviar("recepcion", post("/api/v1/clientes"), cuerpoClienta(nombres, telefonoNuevo())).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(respuesta, "$.idCliente")).longValue();
	}

	private String cuerpoCita(Long idCliente, Long idPersonal, Long idRecurso, Long idVariante, LocalDateTime inicio, int minutos) {
		return "{\"idCliente\":" + idCliente + ",\"idPersonal\":" + idPersonal + ",\"idRecurso\":" + idRecurso + ",\"idVariante\":" + idVariante
				+ ",\"fechaHoraInicio\":\"" + inicio + "\",\"duracionMin\":" + minutos + "}";
	}

	private ResultActions agendar(Long idCliente, Long idPersonal, Long idRecurso, Long idVariante, LocalDateTime inicio, int minutos) throws Exception {
		return enviar("recepcion", post("/api/v1/citas"), cuerpoCita(idCliente, idPersonal, idRecurso, idVariante, inicio, minutos));
	}

	private Long idDe(ResultActions resultado, String campo) throws Exception {
		return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$." + campo)).longValue();
	}

	// ------------------------------------------------------------------ CUS01

	@Test
	void cus01BuscaSoloClientasPorNombreOTelefono() throws Exception {
		String telefono = telefonoNuevo();
		enviar("recepcion", post("/api/v1/clientes"), cuerpoClienta("Rosaura", telefono)).andExpect(status().isCreated());

		enviar("recepcion", get("/api/v1/clientes").param("nombres", "rosaura de"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].telefono").value(telefono))
			.andExpect(jsonPath("$[0].aceptaComunicaciones").value(true));
		enviar("recepcion", get("/api/v1/clientes").param("telefono", telefono), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].nombres").value("Rosaura"));
		// Una persona del personal que no es clienta no sale en la búsqueda
		enviar("recepcion", get("/api/v1/clientes").param("telefono", "987100001"), null).andExpect(status().isOk()).andExpect(jsonPath("$", empty()));
	}

	@Test
	void cus01SinSesionEs401() throws Exception {
		mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.regla").value("SIN_SESION"));
	}

	// ------------------------------------------------------------------ CUS02

	@Test
	void cus02LaConsultaDeDocumentoSigueAManoSiFallaYValidaElNumero() throws Exception {
		Long idDni = id("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'");

		enviar("recepcion", get("/api/v1/clientes/consulta-documento").param("idTipoDocumento", idDni.toString()).param("numeroDocumento", "71234567"), null)
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.resultado").value("ERROR"));
		enviar("recepcion", get("/api/v1/clientes/consulta-documento").param("idTipoDocumento", idDni.toString()).param("numeroDocumento", "123"), null)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN45"));
	}

	@Test
	void cus02CreaPersonaYClienteConElMismoIdEnUnaTransaccion() throws Exception {
		String telefono = telefonoNuevo();
		ResultActions resultado = enviar("recepcion", post("/api/v1/clientes"),
				"{\"nombres\":\"Marisol\",\"apellidos\":\"Quispe\",\"telefono\":\"" + telefono
						+ "\",\"fechaCumpleanos\":\"1990-05-20\",\"observaciones\":\"Alergia al tinte\",\"aceptaComunicaciones\":false}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nombres").value("Marisol"))
			.andExpect(jsonPath("$.origenDatos").value("MANUAL"))
			.andExpect(jsonPath("$.aceptaComunicaciones").value(false))
			.andExpect(jsonPath("$.estado").value("ACTIVO"));
		Long idCliente = idDe(resultado, "idCliente");
		resultado.andExpect(header().string("Location", "/api/v1/clientes/" + idCliente));

		assertThat(id("SELECT COUNT(*) FROM PERSONA WHERE id_persona = ? AND telefono = ?", idCliente, telefono)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT acepta_comunicaciones FROM CLIENTE WHERE id_cliente = ?", String.class, idCliente)).isEqualTo("N");
	}

	@Test
	void cus02ElTelefonoDeOtraClientaEs409SinCrearNada() throws Exception {
		String telefono = telefonoNuevo();
		enviar("recepcion", post("/api/v1/clientes"), cuerpoClienta("Primera", telefono)).andExpect(status().isCreated());
		Long personasAntes = id("SELECT COUNT(*) FROM PERSONA");

		enviar("recepcion", post("/api/v1/clientes"), cuerpoClienta("Segunda", telefono)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RF05"));

		assertThat(id("SELECT COUNT(*) FROM PERSONA")).isEqualTo(personasAntes);
	}

	@Test
	void cus02SinConsentimientoEs400() throws Exception {
		enviar("recepcion", post("/api/v1/clientes"), "{\"nombres\":\"Sin consentimiento\",\"telefono\":\"" + telefonoNuevo() + "\"}")
			.andExpect(status().isBadRequest());
	}

	@Test
	void cus02UnaPersonaDelPersonalPuedeSerClientaConElMismoId() throws Exception {
		Long idRecepcionista = id("SELECT id_personal FROM PERSONAL p JOIN PERSONA e ON e.id_persona = p.id_personal WHERE e.numero_documento = '70000003'");

		enviar("recepcion", post("/api/v1/clientes"), cuerpoClienta("Otro nombre", "987100003")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.idCliente").value(idRecepcionista))
			.andExpect(jsonPath("$.nombres").value("Recepcionista"));
	}

	// ------------------------------------------------------------------ CUS05

	@Test
	void cus05OfreceHorasLibresDeLasEspecialistasDelAreaYQuitaLasOcupadas() throws Exception {
		Long idVariante = idVariante(CORTE);
		LocalDate dia = lunes(10);
		Long idEspecialista = idPersonal("especialista1");
		Long idOtra = idPersonal("especialista2");

		// especialista1 tiene Peluquería; especialista2 no. El servicio es de 30 min desde las 09:00
		enviar("recepcion", get("/api/v1/citas/disponibilidad").param("idVariante", idVariante.toString()).param("fecha", dia.toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].inicio").value(containsString("09:00")))
			.andExpect(jsonPath("$[0].duracionMin").value(30))
			.andExpect(jsonPath("$[0].idsPersonal", hasItem(idEspecialista.intValue())))
			.andExpect(jsonPath("$[0].idsPersonal", not(hasItem(idOtra.intValue()))))
			.andExpect(jsonPath("$[0].idsRecurso", hasItem(recurso("SILLON", 0).intValue())))
			.andExpect(jsonPath("$[0].idsRecurso", not(hasItem(recurso("CAMILLA", 0).intValue()))));

		Long idCliente = registrarClienta("Disponibilidad");
		agendar(idCliente, idEspecialista, recurso("SILLON", 0), idVariante, dia.atTime(9, 0), 30).andExpect(status().isCreated());

		// [It. 2] RN03 y RN51: la cita ocupa a especialista1 de 09:00 a 09:30 y al sillón 0 hasta 09:40 (limpieza de 10 min). Con horas cada
		// AGENDA_PASO_MIN = 15 min, la siguiente libre es 09:30, en otro sillón (en la Iteración 1 eran 10:00: paso de 30 min y limpieza para ella)
		enviar("recepcion", get("/api/v1/citas/disponibilidad").param("idVariante", idVariante.toString()).param("fecha", dia.toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].inicio").value(containsString("09:30")))
			.andExpect(jsonPath("$[0].idsRecurso", not(hasItem(recurso("SILLON", 0).intValue()))));
	}

	@Test
	void cus05UnaCitaEnElPasadoEs422PorRn49() throws Exception {
		Long idCliente = registrarClienta("En el pasado");
		Long citasAntes = id("SELECT COUNT(*) FROM CITA WHERE id_cliente = ?", idCliente);

		agendar(idCliente, idPersonal("especialista1"), recurso("SILLON", 0), idVariante(CORTE), LocalDateTime.now().minusHours(2).withNano(0), 30)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN49"));

		assertThat(id("SELECT COUNT(*) FROM CITA WHERE id_cliente = ?", idCliente)).isEqualTo(citasAntes);
	}

	@Test
	void rn03LaEspecialistaQuedaLibreAlTerminarAunqueElRecursoSigaEnLimpieza() throws Exception {
		Long idCliente = registrarClienta("Con limpieza");
		Long idEspecialista = idPersonal("especialista1");
		Long sillon = recurso("SILLON", 0);
		LocalDate dia = lunes(18);
		agendar(idCliente, idEspecialista, sillon, idVariante(CORTE), dia.atTime(10, 0), 30).andExpect(status().isCreated());

		// Horas libres: a las 10:15 especialista1 aún atiende; a las 10:30 ya está libre, pero el sillón 0 sigue en limpieza hasta las 10:40
		String horas = enviar("recepcion", get("/api/v1/citas/disponibilidad").param("idVariante", idVariante(CORTE).toString()).param("fecha", dia.toString()), null)
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(JsonPath.<List<String>>read(horas, "$[*].inicio")).doesNotContain(dia.atTime(10, 15).toString() + ":00");
		assertThat(JsonPath.<List<Number>>read(horas, "$[?(@.inicio == '" + dia.atTime(10, 30) + ":00')].idsPersonal[*]")).extracting(Number::longValue)
			.contains(idEspecialista);
		assertThat(JsonPath.<List<Number>>read(horas, "$[?(@.inicio == '" + dia.atTime(10, 30) + ":00')].idsRecurso[*]")).extracting(Number::longValue)
			.isNotEmpty()
			.doesNotContain(sillon);

		// Agenda: la especialista queda libre desde las 10:30 y el recurso desde las 10:40
		enviar("recepcion", get("/api/v1/citas/agenda").param("fecha", dia.toString()).param("idPersonal", idEspecialista.toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.franja == 'LIBRE')].inicio", hasItem(dia.atTime(10, 30) + ":00")));
		enviar("recepcion", get("/api/v1/citas/agenda").param("fecha", dia.toString()).param("idRecurso", sillon.toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.franja == 'LIBRE')].inicio", hasItem(dia.atTime(10, 40) + ":00")))
			.andExpect(jsonPath("$[?(@.franja == 'LIBRE')].inicio", not(hasItem(dia.atTime(10, 30) + ":00"))));
	}

	@Test
	void cus05UnaVarianteInexistenteEs404() throws Exception {
		enviar("recepcion", get("/api/v1/citas/disponibilidad").param("idVariante", "999999").param("fecha", lunes(10).toString()), null)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.regla").value("RN40"));
	}

	@Test
	void cus05AgendaYOracleCalculaElFinConLimpieza() throws Exception {
		Long idCliente = registrarClienta("Agendada");
		LocalDateTime inicio = lunes(11).atTime(10, 0);

		ResultActions resultado = agendar(idCliente, idPersonal("especialista1"), recurso("SILLON", 0), idVariante(CORTE), inicio, 30)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.idSede").value(idSede()))
			.andExpect(jsonPath("$.fechaHoraFin").value(containsString("10:40")));

		Long idCita = idDe(resultado, "idCita");
		resultado.andExpect(header().string("Location", "/api/v1/citas/" + idCita));
		assertThat(id("SELECT id_usuario FROM CITA WHERE id_cita = ?", idCita)).isEqualTo(id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'recepcion'"));
	}

	@Test
	void cus05ElCruceDeEspecialistaODeRecursoConLimpiezaEs409() throws Exception {
		Long idCliente = registrarClienta("Con cruce");
		Long idEspecialista = idPersonal("especialista1");
		Long idVariante = idVariante(CORTE);
		LocalDate dia = lunes(12);
		agendar(idCliente, idEspecialista, recurso("SILLON", 0), idVariante, dia.atTime(10, 0), 30).andExpect(status().isCreated());

		// La misma especialista en otro sillón, dentro del servicio (10:00 a 10:30; con la limpieza de 10 min termina 10:40)
		agendar(idCliente, idEspecialista, recurso("SILLON", 1), idVariante, dia.atTime(10, 15), 30).andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RN02"))
			.andExpect(jsonPath("$.detail").value(containsString("especialista")));
		agendar(idCliente, idEspecialista, recurso("SILLON", 1), idVariante, dia.atTime(10, 20), 30).andExpect(status().isConflict());

		// El mismo sillón con otra especialista, dentro de la limpieza: el recurso también choca
		agendar(idCliente, idPersonal("especialista2"), recurso("SILLON", 0), idVariante, dia.atTime(10, 35), 30).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("recurso")));

		// Justo al terminar la limpieza ya no hay cruce
		agendar(idCliente, idEspecialista, recurso("SILLON", 0), idVariante, dia.atTime(10, 40), 30).andExpect(status().isCreated());
	}

	@Test
	void cus05OracleTambienRechazaElCruceSinPasarPorElGestor() throws Exception {
		Long idCliente = registrarClienta("Cruce en Oracle");
		Long idEspecialista = idPersonal("especialista1");
		Long idVariante = idVariante(CORTE);
		LocalDate dia = lunes(13);
		agendar(idCliente, idEspecialista, recurso("SILLON", 0), idVariante, dia.atTime(10, 0), 30).andExpect(status().isCreated());
		SecurityContextHolder.getContext().setAuthentication(sesion("recepcion"));
		Long idUsuario = id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'recepcion'");

		assertThatThrownBy(() -> transacciones.executeWithoutResult(estado -> repositorioCita.saveAndFlush(
				new Cita(idCliente, idSede(), idEspecialista, recurso("SILLON", 1), idVariante, dia.atTime(10, 15), 30, idUsuario))))
			.isInstanceOf(DataAccessException.class)
			.hasStackTraceContaining("ORA-20010");
	}

	@Test
	void cus05UnaEspecialistaSinElAreaDelServicioEs422PorOracle() throws Exception {
		Long idCliente = registrarClienta("Sin área");

		agendar(idCliente, idPersonal("especialista2"), recurso("SILLON", 2), idVariante(CORTE), lunes(14).atTime(10, 0), 30)
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF83"))
			.andExpect(jsonPath("$.detail").value(containsString("no está habilitada")));
	}

	@Test
	void cus05UnaClientaOEspecialistaInexistenteEs422() throws Exception {
		Long idCliente = registrarClienta("Referencias");
		LocalDateTime inicio = lunes(14).atTime(12, 0);

		agendar(999_999L, idPersonal("especialista1"), recurso("SILLON", 0), idVariante(CORTE), inicio, 30).andExpect(status().isUnprocessableContent());
		agendar(idCliente, 999_999L, recurso("SILLON", 0), idVariante(CORTE), inicio, 30).andExpect(status().isUnprocessableContent());
		agendar(idCliente, idPersonal("especialista1"), 999_999L, idVariante(CORTE), inicio, 30).andExpect(status().isUnprocessableContent());
	}

	@Test
	void cus05LaEspecialistaNoPuedeAgendar() throws Exception {
		enviar("especialista1", post("/api/v1/citas"), cuerpoCita(1L, 1L, 1L, 1L, lunes(14).atTime(12, 0), 30)).andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ CUS06

	@Test
	void cus06AnotaEnEsperaYDevuelveLasHorasLibres() throws Exception {
		Long idCliente = registrarClienta("En espera");
		LocalDate dia = lunes(15);

		ResultActions resultado = enviar("recepcion", post("/api/v1/citas/lista-espera"), "{\"idCliente\":" + idCliente + ",\"idVariante\":" + idVariante(CORTE)
				+ ",\"fechaDeseada\":\"" + dia + "\",\"horaDesde\":\"15:00\",\"horaHasta\":\"18:00\"}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.idSede").value(idSede()))
			.andExpect(jsonPath("$.horaDesde").value("15:00"))
			.andExpect(jsonPath("$.horasLibres[0].inicio").value(containsString("09:00")));

		assertThat(id("SELECT COUNT(*) FROM LISTA_ESPERA WHERE id_espera = ? AND id_cliente = ?", idDe(resultado, "idEspera"), idCliente)).isEqualTo(1);
	}

	@Test
	void cus06UnaClientaInexistenteEs422YUnaHoraMalEscritaEs400() throws Exception {
		String cuerpo = "{\"idCliente\":999999,\"idVariante\":" + idVariante(CORTE) + ",\"fechaDeseada\":\"" + lunes(15) + "\"";

		enviar("recepcion", post("/api/v1/citas/lista-espera"), cuerpo + "}").andExpect(status().isUnprocessableContent());
		enviar("recepcion", post("/api/v1/citas/lista-espera"), cuerpo + ",\"horaDesde\":\"25:00\"}").andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------ CUS08

	@Test
	void cus08LaAgendaTraeBloquesOcupadosConLimpiezaYFranjasLibres() throws Exception {
		Long idCliente = registrarClienta("De agenda");
		Long idEspecialista = idPersonal("especialista1");
		Long idRecurso = recurso("SILLON", 3);
		LocalDate dia = lunes(16);
		Long idCita = idDe(agendar(idCliente, idEspecialista, idRecurso, idVariante(CORTE), dia.atTime(10, 0), 30).andExpect(status().isCreated()), "idCita");

		enviar("recepcion", get("/api/v1/citas/agenda").param("fecha", dia.toString()).param("idPersonal", idEspecialista.toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].franja").value("LIBRE"))
			.andExpect(jsonPath("$[0].inicio").value(containsString("09:00")))
			.andExpect(jsonPath("$[0].fin").value(containsString("10:00")))
			.andExpect(jsonPath("$[1].franja").value("OCUPADA"))
			.andExpect(jsonPath("$[1].origen").value("CITA"))
			.andExpect(jsonPath("$[1].idOrigen").value(idCita))
			.andExpect(jsonPath("$[1].fin").value(containsString("10:40")))
			.andExpect(jsonPath("$[1].finServicio").value(containsString("10:30")))
			.andExpect(jsonPath("$[1].nombreCliente").value("De agenda De Prueba"))
			.andExpect(jsonPath("$[1].nombreServicio").value(CORTE))
			.andExpect(jsonPath("$[1].nombreEspecialista").isNotEmpty())
			// [It. 2] RN03: la especialista queda libre al terminar (10:30), aunque el sillón siga en limpieza hasta las 10:40
			.andExpect(jsonPath("$[2].franja").value("LIBRE"))
			.andExpect(jsonPath("$[2].inicio").value(containsString("10:30")))
			.andExpect(jsonPath("$[2].fin").value(containsString("19:00")));

		enviar("recepcion", get("/api/v1/citas/agenda").param("fecha", dia.toString()).param("idRecurso", idRecurso.toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[2].idRecurso").value(idRecurso))
			.andExpect(jsonPath("$[2].fin").value(containsString("20:00")));

		// Sin filtros: el bloque, dos franjas de especialista1, una de especialista2 y una por recurso, más la que parte el sillón
		int recursos = id("SELECT COUNT(*) FROM RECURSO WHERE estado = 'DISPONIBLE' AND id_sede = ?", idSede()).intValue();
		enviar("especialista1", get("/api/v1/citas/agenda").param("fecha", dia.toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1 + 2 + 1 + recursos + 1)));
	}

	@Test
	void cus08LosFiltrosTraenLasEspecialistasYLosRecursosDeLaSede() throws Exception {
		Long idEspecialista = idPersonal("especialista1");
		Long recursos = id("SELECT COUNT(*) FROM RECURSO WHERE id_sede = ?", idSede());

		enviar("recepcion", get("/api/v1/citas/filtros"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.especialistas[*].idPersonal", hasItem(idEspecialista.intValue())))
			.andExpect(jsonPath("$.especialistas[?(@.idPersonal == " + idEspecialista + ")].nombres").isNotEmpty())
			.andExpect(jsonPath("$.especialistas[*].idPersonal", not(hasItem(idPersonal("recepcion").intValue()))))
			.andExpect(jsonPath("$.recursos", hasSize(recursos.intValue())))
			.andExpect(jsonPath("$.recursos[0].tipo").isNotEmpty());
	}

	@Test
	void cus08SinFechaEs400() throws Exception {
		enviar("recepcion", get("/api/v1/citas/agenda"), null).andExpect(status().isBadRequest());
	}

	// ------------------------------------------------------------------ CUS11

	@Test
	void cus11IniciaYFinalizaLaAtencionDeLaCita() throws Exception {
		Long idCliente = registrarClienta("Atendida");
		Long idEspecialista = idPersonal("especialista1");
		Long idRecurso = recurso("SILLON", 4);
		LocalDate dia = lunes(17);
		Long idCita = idDe(agendar(idCliente, idEspecialista, idRecurso, idVariante(CORTE), dia.atTime(10, 0), 30).andExpect(status().isCreated()), "idCita");

		ResultActions inicio = enviar("especialista1", post("/api/v1/atenciones"), "{\"idCita\":" + idCita + ",\"tiempoDeclaradoMin\":45}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("EN_CURSO"))
			.andExpect(jsonPath("$.idCliente").value(idCliente))
			.andExpect(jsonPath("$.idPersonal").value(idEspecialista))
			.andExpect(jsonPath("$.tiempoDeclaradoMin").value(45));
		Long idAtencion = idDe(inicio, "idAtencion");
		inicio.andExpect(header().string("Location", "/api/v1/atenciones/" + idAtencion));

		// RF13: la cita pasa a EN_ATENCION y el tiempo declarado actualiza su bloque, con la limpieza (45 + 10)
		assertThat(jdbc.queryForObject("SELECT estado FROM CITA WHERE id_cita = ?", String.class, idCita)).isEqualTo("EN_ATENCION");
		assertThat(jdbc.queryForObject("SELECT TO_CHAR(fecha_hora_fin, 'HH24:MI') FROM CITA WHERE id_cita = ?", String.class, idCita)).isEqualTo("10:55");

		// [It. 2] Mi agenda sale de la sesión y trae la atención EN_CURSO; en-curso la retoma desde otro equipo
		enviar("especialista1", get("/api/v1/atenciones/mi-agenda").param("fecha", dia.toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.idCita == " + idCita + ")].idAtencion", hasItem(idAtencion.intValue())))
			.andExpect(jsonPath("$[?(@.idCita == " + idCita + ")].estadoAtencion", hasItem("EN_CURSO")));
		enviar("especialista2", get("/api/v1/atenciones/mi-agenda").param("fecha", dia.toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idCita", not(hasItem(idCita.intValue()))));
		enviar("especialista1", get("/api/v1/atenciones/en-curso").param("idCita", idCita.toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.idAtencion").value(idAtencion));

		// [It. 2] RN50: otra especialista no la finaliza
		enviar("especialista2", patch("/api/v1/atenciones/" + idAtencion + "/fin"), "{\"observaciones\":\"Ajena\"}").andExpect(status().isForbidden())
			.andExpect(jsonPath("$.regla").value("RN50"));

		// Una atención no se inicia dos veces
		enviar("especialista1", post("/api/v1/atenciones"), "{\"idCita\":" + idCita + ",\"tiempoDeclaradoMin\":45}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF13"));

		enviar("especialista1", patch("/api/v1/atenciones/" + idAtencion + "/fin"), "{\"observaciones\":\"Sin novedad\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("FINALIZADA"))
			.andExpect(jsonPath("$.observaciones").value("Sin novedad"))
			.andExpect(jsonPath("$.horaFin").exists())
			// Rama de paquete: sin datos en PAQUETE_SERVICIO no hay paquete y la atención queda para cobrar
			.andExpect(jsonPath("$.idClientePaquete").doesNotExist());

		assertThat(jdbc.queryForObject("SELECT estado FROM CITA WHERE id_cita = ?", String.class, idCita)).isEqualTo("FINALIZADA");
		enviar("especialista1", get("/api/v1/atenciones/en-curso").param("idCita", idCita.toString()), null).andExpect(status().isNotFound());

		// Finalizar otra vez no se permite
		enviar("especialista1", patch("/api/v1/atenciones/" + idAtencion + "/fin"), "{\"observaciones\":\"Otra vez\"}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF13"));

		// AtencionApi: lo que ventas leerá en B4
		SecurityContextHolder.getContext().setAuthentication(sesion("recepcion"));
		AtencionCobrable cobrable = atencionApi.obtenerAtencionCobrable(idAtencion);
		assertThat(cobrable.idCliente()).isEqualTo(idCliente);
		assertThat(cobrable.idClientePaquete()).isNull();
	}

	@Test
	void cus11UnaEspecialistaAjenaNoIniciaLaAtencionPorRn50() throws Exception {
		Long idCliente = registrarClienta("Ajena");
		Long idCita = idDe(agendar(idCliente, idPersonal("especialista1"), recurso("SILLON", 4), idVariante(CORTE), lunes(19).atTime(10, 0), 30)
			.andExpect(status().isCreated()), "idCita");

		enviar("especialista2", post("/api/v1/atenciones"), "{\"idCita\":" + idCita + ",\"tiempoDeclaradoMin\":30}")
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.regla").value("RN50"));

		assertThat(jdbc.queryForObject("SELECT estado FROM CITA WHERE id_cita = ?", String.class, idCita)).isEqualTo("PENDIENTE");
		assertThat(id("SELECT COUNT(*) FROM ATENCION WHERE id_cita = ?", idCita)).isZero();
	}

	@Test
	void cus11UnaCitaInexistenteEs404() throws Exception {
		enviar("especialista1", post("/api/v1/atenciones"), "{\"idCita\":999999,\"tiempoDeclaradoMin\":45}").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.regla").value("RF13"));
	}

	@Test
	void cus11FinalizarUnaAtencionInexistenteEs404() throws Exception {
		enviar("especialista1", patch("/api/v1/atenciones/999999/fin"), "{\"observaciones\":\"Nada\"}").andExpect(status().isNotFound());
	}

	@Test
	void cus11LaRecepcionistaNoFinalizaAtenciones() throws Exception {
		enviar("recepcion", patch("/api/v1/atenciones/999999/fin"), "{\"observaciones\":\"Nada\"}").andExpect(status().isForbidden());
	}

	@Test
	void cus11LasLecturasDeLaRamaDePaqueteCorrenEnOracle() {
		SecurityContextHolder.getContext().setAuthentication(sesion("recepcion"));
		transacciones.executeWithoutResult(estado -> {
			assertThat(repositorioClientePaquete.findActivoPorCliente(999_999L, 999_999L)).isEmpty();
			assertThat(repositorioClientePaquete.contarSesionesUsadas(999_999L)).isNull();
		});
	}

	@Test
	void atencionApiRegistraSinCitaConClientaDePasoYSoloEntregaLaFinalizada() {
		SecurityContextHolder.getContext().setAuthentication(sesion("recepcion"));
		Long idEspecialista = idPersonal("especialista2");
		Long idVarianteFacial = idVariante("Perfilado de Cejas");

		// Con rollback: MapeoEntidadesTest espera que hoy no haya atenciones sin cita
		transacciones.executeWithoutResult(estado -> {
			assertThat(atencionApi.libresAhora(idVarianteFacial, idSede())).allSatisfy(hora -> assertThat(hora.idsPersonal()).contains(idEspecialista));
			Long idAtencion = atencionApi.registrarSinCita(null, "Clienta de paso", idEspecialista, recurso("CABINA", 0), idVarianteFacial, 20);

			assertThat(id("SELECT COUNT(*) FROM ATENCION WHERE id_atencion = ? AND id_cliente IS NULL AND estado = 'EN_CURSO'", idAtencion)).isEqualTo(1);
			assertThatThrownBy(() -> atencionApi.obtenerAtencionCobrable(idAtencion)).isInstanceOf(ReglaNegocioException.class);
			estado.setRollbackOnly();
		});
		assertThatThrownBy(() -> atencionApi.obtenerAtencionCobrable(999_999L)).isInstanceOf(ReglaNegocioException.class);
	}

}
