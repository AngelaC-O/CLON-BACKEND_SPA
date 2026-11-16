package dev.com.Synapsis_Spa_Cautiva;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.UsuarioSesion;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Apoyo de las pruebas de caja y ventas. Cada clase trabaja en una sede propia, con su especialista y sus series: los datos se confirman y
 * MapeoEntidadesTest espera la sede del script 15 sin caja abierta ni atenciones sin cita. Las atenciones finalizadas las inserta el dueño
 * en días pasados, porque una atención deja ocupada a la especialista.
 */
abstract class VentasFlujoApoyo extends OracleTestcontainer {

	protected static final String CORTE = "Corte de Cabello Caballero";

	protected static final BigDecimal PRECIO_CORTE = new BigDecimal("35");

	private static final String TOKEN_CSRF = "token-de-prueba";

	private static final AtomicInteger SECUENCIA = new AtomicInteger(1);

	private static final AtomicInteger DIAS_ATRAS = new AtomicInteger(1);

	private static final Map<String, Long> SEDES = new ConcurrentHashMap<>();

	private static final Map<Long, Long> ESPECIALISTAS = new ConcurrentHashMap<>();

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JdbcTemplate jdbc;

	// ------------------------------------------------------------------ datos de la sede

	protected Long sedeDeLaClase() throws SQLException {
		String clase = getClass().getSimpleName();
		Long idSede = SEDES.get(clase);
		if (idSede == null) {
			idSede = sedeNueva(clase);
			prepararVentas(idSede);
			SEDES.put(clase, idSede);
		}
		return idSede;
	}

	/** Sin caja, sin series y sin personal: para los casos que dependen de eso. */
	protected Long sedeNueva(String nombre) throws SQLException {
		String nombreUnico = nombre + " " + SECUENCIA.incrementAndGet();
		comoDueno("INSERT INTO SEDE (nombre, direccion, horario_apertura, horario_cierre) VALUES ('" + nombreUnico + "', 'De prueba', '09:00', '20:00')");
		return id("SELECT id_sede FROM SEDE WHERE nombre = ?", nombreUnico);
	}

	private void prepararVentas(Long idSede) throws SQLException {
		int numero = SECUENCIA.incrementAndGet();
		String sufijo = String.format("%03d", numero % 1000);
		comoDueno("INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (" + idSede + ", 'BOLETA', 'B" + sufijo + "')");
		comoDueno("INSERT INTO SERIE_COMPROBANTE (id_sede, tipo, serie) VALUES (" + idSede + ", 'FACTURA', 'F" + sufijo + "')");
		for (int i = 1; i <= 3; i++) {
			comoDueno("INSERT INTO RECURSO (id_sede, nombre, tipo) VALUES (" + idSede + ", 'Sillón " + i + "', 'SILLON')");
		}
		String dni = String.format("71%06d", numero);
		String telefono = String.format("98720%04d", numero);
		comoDueno("INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono) "
				+ "SELECT 'NATURAL', 'Especialista', 'De Ventas', id_tipo_documento, '" + dni + "', '" + telefono + "' FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'");
		Long idPersonal = id("SELECT id_persona FROM PERSONA WHERE numero_documento = ?", dni);
		comoDueno("INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso) VALUES (" + idPersonal
				+ ", " + idSede + ", 'ESPECIALISTA_TECNICA', 'MIXTO', 800, 20, TRUNC(SYSDATE))");
		for (int dia = 1; dia <= 7; dia++) {
			comoDueno("INSERT INTO HORARIO_PERSONAL (id_personal, dia_semana, hora_entrada, hora_salida, minutos_almuerzo) VALUES (" + idPersonal + ", " + dia
					+ ", '00:00', '23:59', 0)");
		}
		comoDueno("INSERT INTO PERSONAL_AREA (id_personal, id_area) SELECT " + idPersonal + ", id_area FROM AREA WHERE nombre LIKE 'Peluquer%'");
		ESPECIALISTAS.put(idSede, idPersonal);
	}

	protected Long especialista() throws SQLException {
		return ESPECIALISTAS.get(sedeDeLaClase());
	}

	protected Long recurso(int posicion) throws SQLException {
		return jdbc.queryForList("SELECT id_recurso FROM RECURSO WHERE id_sede = ? ORDER BY id_recurso", Long.class, sedeDeLaClase()).get(posicion);
	}

	protected Long medio(String nombre) {
		return id("SELECT id_medio_pago FROM MEDIO_PAGO WHERE nombre = ?", nombre);
	}

	protected Long variante(String servicio) {
		return id("SELECT v.id_variante FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio WHERE s.nombre = ?", servicio);
	}

	protected LocalDate lunes(int semana) {
		return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(semana);
	}

	/** Atención FINALIZADA en un día pasado propio. Sin clienta, es de paso. */
	protected Long atencionFinalizada(Long idCliente, Long idVariante) throws SQLException {
		int dias = DIAS_ATRAS.incrementAndGet();
		String cliente = idCliente == null ? "NULL, 'Clienta de paso'" : idCliente + ", NULL";
		comoDueno("INSERT INTO ATENCION (id_cliente, nombre_cliente_paso, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES (" + cliente
				+ ", " + especialista() + ", " + recurso(0) + ", " + idVariante + ", CAST(SYSTIMESTAMP AS TIMESTAMP) - " + dias + ", 30)");
		Long idAtencion = id("SELECT MAX(id_atencion) FROM ATENCION");
		comoDueno("UPDATE ATENCION SET estado = 'FINALIZADA', hora_fin = hora_inicio + INTERVAL '30' MINUTE WHERE id_atencion = " + idAtencion);
		return idAtencion;
	}

	protected void comoDueno(String sql) throws SQLException {
		try (Connection dueno = DriverManager.getConnection(ORACLE.getJdbcUrl(), DUENO, CLAVE_DUENO); Statement sentencia = dueno.createStatement()) {
			sentencia.executeUpdate(sql);
		}
	}

	protected Long id(String sql, Object... parametros) {
		return jdbc.queryForObject(sql, Long.class, parametros);
	}

	// ------------------------------------------------------------------ peticiones

	protected Authentication sesion(String usuario, Long idSede) {
		Long idUsuario = id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = ?", usuario);
		List<String> permisos = jdbc.queryForList("""
				SELECT p.modulo || ':' || p.accion FROM USUARIO u
				JOIN ROL_PERMISO rp ON rp.id_rol = u.id_rol
				JOIN PERMISO p ON p.id_permiso = rp.id_permiso
				WHERE u.nombre_usuario = ?
				""", String.class, usuario);
		UsuarioSesion principal = new UsuarioSesion(idUsuario, usuario, idSede, false, new HashSet<>(permisos));
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, permisos.stream().map(SimpleGrantedAuthority::new).toList());
	}

	/** Cookie y cabecera como las manda Angular (ver las notas de B3). */
	protected ResultActions enviarEnSede(String usuario, Long idSede, MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		pedido.with(authentication(sesion(usuario, idSede))).cookie(new Cookie("XSRF-TOKEN", TOKEN_CSRF)).header("X-XSRF-TOKEN", TOKEN_CSRF);
		if (cuerpo != null) {
			pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
		}
		return mvc.perform(pedido);
	}

	protected ResultActions enviar(MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		return enviarEnSede("recepcion", sedeDeLaClase(), pedido, cuerpo);
	}

	protected Long idDe(ResultActions resultado, String campo) throws Exception {
		return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$." + campo)).longValue();
	}

	protected Long clienta(String nombres) throws Exception {
		String telefono = String.format("98721%04d", SECUENCIA.incrementAndGet());
		return idDe(enviar(post("/api/v1/clientes"), "{\"nombres\":\"" + nombres + "\",\"telefono\":\"" + telefono + "\",\"aceptaComunicaciones\":true}")
			.andExpect(status().isCreated()), "idCliente");
	}

	/** La caja abierta de la sede de la clase; la abre si no hay. */
	protected Long cajaAbierta() throws Exception {
		List<Long> abiertas = jdbc.queryForList("SELECT id_caja FROM CAJA WHERE estado = 'ABIERTA' AND id_sede = ?", Long.class, sedeDeLaClase());
		if (!abiertas.isEmpty()) {
			return abiertas.getFirst();
		}
		return idDe(enviar(post("/api/v1/cajas"), "{\"montoInicial\":100}").andExpect(status().isCreated()), "idCaja");
	}

	protected ResultActions crearVenta(Long idAtencion) throws Exception {
		return enviar(post("/api/v1/ventas"), "{\"idAtencion\":" + idAtencion + "}");
	}

	protected ResultActions pagar(Long idVenta, Long idMedioPago, String monto, String canal) throws Exception {
		String conCanal = canal == null ? "" : ",\"canal\":\"" + canal + "\"";
		return enviar(post("/api/v1/ventas/" + idVenta + "/pagos"),
				"{\"pagos\":[{\"idMedioPago\":" + idMedioPago + ",\"monto\":" + monto + conCanal + "}]}");
	}

	protected ResultActions cerrarConBoleta(Long idVenta) throws Exception {
		return enviar(post("/api/v1/ventas/" + idVenta + "/cierre"), "{\"tipo\":\"BOLETA\"}");
	}

	/** Venta cerrada en efectivo de una atención nueva de clienta de paso. */
	protected ResultActions ventaCerrada() throws Exception {
		cajaAbierta();
		Long idVenta = idDe(crearVenta(atencionFinalizada(null, variante(CORTE))).andExpect(status().isCreated()), "idVenta");
		pagar(idVenta, medio("EFECTIVO"), PRECIO_CORTE.toPlainString(), null).andExpect(status().isOk());
		return cerrarConBoleta(idVenta).andExpect(status().isOk());
	}

}
