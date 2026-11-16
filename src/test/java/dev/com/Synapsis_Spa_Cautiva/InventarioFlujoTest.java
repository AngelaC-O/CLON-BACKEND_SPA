package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.UsuarioSesion;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * [It. 2] CUS66, CUS32, CUS33, CUS34, CUS35 y el consumo automático de insumos de CUS11 (RN53, RN54), con la usuaria almacen del script 17.
 * La base es compartida y los datos se confirman: cada prueba crea sus propios productos y servicios, sus citas van a semanas y sillones que
 * no usan otras clases, y todo consumo pendiente que crea queda resuelto al terminar (MapeoEntidadesTest espera que no haya pendientes).
 */
@SpringBootTest
@AutoConfigureMockMvc
class InventarioFlujoTest extends OracleTestcontainer {

	private static final AtomicInteger CODIGOS = new AtomicInteger(100);

	private static final AtomicInteger TELEFONOS = new AtomicInteger(987_630_000);

	private static final String TOKEN_CSRF = "token-de-prueba";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	// ------------------------------------------------------------------ apoyo

	private Long id(String sql, Object... parametros) {
		return jdbc.queryForObject(sql, Long.class, parametros);
	}

	private Long idSede() {
		return id("SELECT p.id_sede FROM USUARIO u JOIN PERSONAL p ON p.id_personal = u.id_personal WHERE u.nombre_usuario = 'almacen'");
	}

	private Long idCategoriaInsumos() {
		return id("SELECT id_categoria FROM CATEGORIA_PRODUCTO WHERE nombre = 'Insumos de servicio'");
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

	private ResultActions enviar(String usuario, MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		pedido.with(authentication(sesion(usuario))).cookie(new Cookie("XSRF-TOKEN", TOKEN_CSRF)).header("X-XSRF-TOKEN", TOKEN_CSRF);
		if (cuerpo != null) {
			pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
		}
		return mvc.perform(pedido);
	}

	private static Long idDe(ResultActions resultado, String campo) throws Exception {
		return ((Number) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$." + campo)).longValue();
	}

	private static String codigoNuevo() {
		return "TIN-" + CODIGOS.incrementAndGet();
	}

	private Integer stock(Long idProducto) {
		return jdbc.queryForObject("SELECT stock_actual FROM INVENTARIO_SEDE WHERE id_producto = ? AND id_sede = ?", Integer.class, idProducto,
				idSede());
	}

	/** CUS32 y CUS34: un insumo en ml con stock mínimo 1 y, si se pide, el stock inicial por un ajuste de conteo. */
	private Long insumoConStock(String nombre, int stockInicial) throws Exception {
		String cuerpo = "{\"codigo\":\"" + codigoNuevo() + "\",\"nombre\":\"" + nombre + "\",\"idCategoria\":" + idCategoriaInsumos()
				+ ",\"unidad\":\"ml\",\"tipoUso\":\"INSUMO\",\"costo\":2.00,\"stockMinimo\":1,\"ubicacion\":\"Estante A\"}";
		Long idProducto = idDe(enviar("almacen", post("/api/v1/productos"), cuerpo).andExpect(status().isCreated()), "idProducto");
		if (stockInicial > 0) {
			ajustar(idProducto, stockInicial).andExpect(status().isOk());
		}
		return idProducto;
	}

	private ResultActions ajustar(Long idProducto, int contado) throws Exception {
		return enviar("almacen", post("/api/v1/productos/" + idProducto + "/ajustes"), "{\"cantidad\":" + contado + ",\"motivo\":\"Conteo físico\"}");
	}

	/**
	 * CUS57, CUS61, CUS05 y CUS11: un servicio de Cuidado Facial con el insumo en su variante, una cabina propia, una cita de especialista2 y
	 * la atención iniciada. Devuelve el id de la atención EN_CURSO. La cabina propia y especialista2 (ninguna otra clase inicia atenciones
	 * suyas ahora) evitan cruces con otras pruebas. [It. 2.1] Sin tiempo de limpieza (RN03 v5.2): la cabina ya no lo declara.
	 */
	private Long atencionEnCursoConInsumo(String servicio, Long idInsumo, String cantidadEstimada, int semana) throws Exception {
		Long idArea = id("SELECT id_area FROM AREA WHERE nombre = 'Cuidado Facial'");
		String cuerpoServicio = "{\"idArea\":" + idArea + ",\"nombre\":\"" + servicio + "\",\"duracionRefMin\":30,\"tipoRecurso\":\"CABINA\","
				+ "\"variantes\":[{\"nombreVariante\":\"Única\",\"precio\":40.00,\"insumos\":[{\"idProducto\":" + idInsumo + ",\"cantidadEstimada\":"
				+ cantidadEstimada + "}]}]}";
		enviar("admin", post("/api/v1/servicios"), cuerpoServicio).andExpect(status().isCreated());
		Long idVariante = id("SELECT v.id_variante FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio WHERE s.nombre = ?",
				servicio);

		String clienta = "{\"nombres\":\"Con insumo\",\"apellidos\":\"De Prueba\",\"telefono\":\"" + TELEFONOS.incrementAndGet()
				+ "\",\"aceptaComunicaciones\":true}";
		Long idCliente = idDe(enviar("recepcion", post("/api/v1/clientes"), clienta).andExpect(status().isCreated()), "idCliente");
		String recurso = "{\"idSede\":" + idSede() + ",\"nombre\":\"Cabina " + servicio + "\",\"tipo\":\"CABINA\"}";
		Long idRecurso = idDe(enviar("admin", post("/api/v1/recursos"), recurso).andExpect(status().isCreated()), "idRecurso");
		LocalDate dia = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(semana);
		String cita = "{\"idCliente\":" + idCliente + ",\"idPersonal\":" + id("SELECT id_personal FROM USUARIO WHERE nombre_usuario = 'especialista2'")
				+ ",\"idRecurso\":" + idRecurso + ",\"idVariante\":" + idVariante + ",\"fechaHoraInicio\":\"" + dia.atTime(10, 0) + "\",\"duracionMin\":30}";
		Long idCita = idDe(enviar("recepcion", post("/api/v1/citas"), cita).andExpect(status().isCreated()), "idCita");
		ResultActions inicio = enviar("especialista2", post("/api/v1/atenciones"), "{\"idCita\":" + idCita + ",\"tiempoDeclaradoMin\":30}");
		assertThat(inicio.andReturn().getResponse().getStatus()).as(inicio.andReturn().getResponse().getContentAsString()).isEqualTo(201);
		return idDe(inicio, "idAtencion");
	}

	private ResultActions finalizar(Long idAtencion) throws Exception {
		return enviar("especialista2", patch("/api/v1/atenciones/" + idAtencion + "/fin"), "{\"observaciones\":\"Sin novedad\"}");
	}

	// ------------------------------------------------------------------ CUS66

	@Test
	void cus66RegistraLaCategoriaYElNombreRepetidoEs409() throws Exception {
		enviar("almacen", get("/api/v1/categorias-producto"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", hasItem("Insumos de servicio")));

		String cuerpo = "{\"nombre\":\"Accesorios de prueba\"}";
		enviar("almacen", post("/api/v1/categorias-producto"), cuerpo).andExpect(status().isCreated())
			.andExpect(jsonPath("$.idCategoria").exists())
			.andExpect(jsonPath("$.nombre").value("Accesorios de prueba"));
		enviar("almacen", post("/api/v1/categorias-producto"), cuerpo).andExpect(status().isConflict());
		assertThat(id("SELECT COUNT(*) FROM CATEGORIA_PRODUCTO WHERE nombre = 'Accesorios de prueba'")).isEqualTo(1);

		enviar("recepcion", post("/api/v1/categorias-producto"), "{\"nombre\":\"Sin permiso\"}").andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ CUS32

	@Test
	void cus32CalculaElPrecioConElMargenYSoloLaAdministradoraVeElMargen() throws Exception {
		Long idCategoria = id("SELECT id_categoria FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Capilar'");
		String codigo = codigoNuevo();
		String cuerpo = "{\"codigo\":\"" + codigo + "\",\"nombre\":\"Mascarilla de prueba\",\"marca\":\"Marca T\",\"idCategoria\":" + idCategoria
				+ ",\"unidad\":\"und\",\"tipoUso\":\"VENTA\",\"costo\":20.00,\"stockMinimo\":2,\"ubicacion\":\"Vitrina 1\"}";

		// RF87: MARGEN_PRODUCTO_PCT = 50 → 20 × 1,5; RN24: la encargada ve el costo, no el margen; RF86: nace con stock 0 en su sede
		Long idProducto = idDe(enviar("almacen", post("/api/v1/productos"), cuerpo).andExpect(status().isCreated())
			.andExpect(jsonPath("$.precioVenta").value(30.00))
			.andExpect(jsonPath("$.costo").value(20.00))
			.andExpect(jsonPath("$.margen").doesNotExist())
			.andExpect(jsonPath("$.unidad").value("UND"))
			.andExpect(jsonPath("$.stockActual").value(0))
			.andExpect(jsonPath("$.stockMinimo").value(2))
			.andExpect(jsonPath("$.ubicacion").value("Vitrina 1")), "idProducto");
		assertThat(stock(idProducto)).isZero();

		enviar("admin", get("/api/v1/productos").param("nombre", "mascarilla de prueba").param("idCategoria", idCategoria.toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].margen").value(50.00));
		enviar("almacen", get("/api/v1/productos").param("nombre", "mascarilla de prueba"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].margen").doesNotExist());

		// Premium: MARGEN_PREMIUM_PCT = 100; y la encargada puede ajustar el margen antes de guardar
		enviar("almacen", post("/api/v1/productos"), cuerpo.replace(codigo, codigoNuevo()).replace("Mascarilla de prueba", "Premium de prueba")
			.replace("\"costo\":20.00", "\"costo\":10.00,\"esPremium\":true")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.precioVenta").value(20.00))
			.andExpect(jsonPath("$.esPremium").value(true));
		enviar("almacen", post("/api/v1/productos"), cuerpo.replace(codigo, codigoNuevo()).replace("Mascarilla de prueba", "Ajustado de prueba")
			.replace("\"costo\":20.00", "\"costo\":10.00,\"margen\":25")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.precioVenta").value(12.50));

		enviar("especialista1", post("/api/v1/productos"), cuerpo).andExpect(status().isForbidden());
	}

	@Test
	void cus32UnInsumoFueraDeSuUnidadMinimaEs422PorRn54() throws Exception {
		String cuerpo = "{\"codigo\":\"" + codigoNuevo() + "\",\"nombre\":\"Tinte en litros\",\"idCategoria\":" + idCategoriaInsumos()
				+ ",\"unidad\":\"litro\",\"tipoUso\":\"INSUMO\",\"costo\":30.00,\"stockMinimo\":1}";
		enviar("almacen", post("/api/v1/productos"), cuerpo).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN54"))
			.andExpect(jsonPath("$.detail", containsString("unidad mínima")));
		assertThat(id("SELECT COUNT(*) FROM PRODUCTO WHERE nombre = 'Tinte en litros'")).isZero();
	}

	// ------------------------------------------------------------------ CUS33, CUS34 y CUS35

	@Test
	void cus34Cus33Cus35AjusteSalidaYKardexConSuSaldo() throws Exception {
		Long idProducto = insumoConStock("Oxidante de prueba", 0);

		// CUS34: contado 5 con stock 0 → AJUSTE +5; contar lo mismo no registra nada
		ajustar(idProducto, 5).andExpect(status().isOk())
			.andExpect(jsonPath("$.tipo").value("AJUSTE"))
			.andExpect(jsonPath("$.cantidad").value(5))
			.andExpect(jsonPath("$.saldo").value(5))
			.andExpect(jsonPath("$.estaEnMinimo").value(false));
		ajustar(idProducto, 5).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RF48"));

		// CUS33: la salida deja 1, que es el mínimo (RF47)
		enviar("almacen", post("/api/v1/productos/" + idProducto + "/salidas"), "{\"cantidad\":4,\"motivo\":\"Uso en cabina\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tipo").value("SALIDA"))
			.andExpect(jsonPath("$.saldo").value(1))
			.andExpect(jsonPath("$.estaEnMinimo").value(true));
		assertThat(stock(idProducto)).isEqualTo(1);

		// RN15: sin stock suficiente es 409 y no se mueve nada
		enviar("almacen", post("/api/v1/productos/" + idProducto + "/salidas"), "{\"cantidad\":2,\"motivo\":\"Uso en cabina\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RN15"));
		// RN17: la recepcionista no registra movimientos
		enviar("recepcion", post("/api/v1/productos/" + idProducto + "/salidas"), "{\"cantidad\":1,\"motivo\":\"Uso en cabina\"}")
			.andExpect(status().isForbidden());
		assertThat(stock(idProducto)).isEqualTo(1);

		// CUS35: V_KARDEX con la cantidad con signo, el saldo y el usuario
		enviar("almacen", get("/api/v1/productos/" + idProducto + "/kardex").param("fechaInicio", LocalDate.now().toString())
			.param("fechaFin", LocalDate.now().toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].tipo").value("AJUSTE"))
			.andExpect(jsonPath("$[0].cantidad").value(5))
			.andExpect(jsonPath("$[1].tipo").value("SALIDA"))
			.andExpect(jsonPath("$[1].cantidad").value(-4))
			.andExpect(jsonPath("$[1].saldo").value(1))
			.andExpect(jsonPath("$[1].usuario").exists());
		enviar("almacen", get("/api/v1/productos/" + idProducto + "/kardex").param("fechaInicio", LocalDate.now().toString())
			.param("fechaFin", LocalDate.now().minusDays(1).toString()), null).andExpect(status().isUnprocessableContent());
		enviar("recepcion", get("/api/v1/productos/" + idProducto + "/kardex"), null).andExpect(status().isForbidden());
	}

	@Test
	void cus33UnaSalidaDeUnProductoInexistenteEs404() throws Exception {
		enviar("almacen", post("/api/v1/productos/999999/salidas"), "{\"cantidad\":1,\"motivo\":\"Nada\"}").andExpect(status().isNotFound());
		enviar("almacen", post("/api/v1/productos/999999/ajustes"), "{\"cantidad\":1,\"motivo\":\"Nada\"}").andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------------ CUS11 (consumo automático) y CUS33 (pendientes)

	@Test
	void cus11ConStockSuficienteDescuentaElConsumoEstimadoSinPendientes() throws Exception {
		Long idInsumo = insumoConStock("Champú de lavado de prueba", 10);
		Long idAtencion = atencionEnCursoConInsumo("Limpieza con insumo suficiente", idInsumo, "2.5", 40);

		// RN54: 2,5 ml se descuentan redondeados hacia arriba a 3
		finalizar(idAtencion).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("FINALIZADA"))
			.andExpect(jsonPath("$.consumosPendientes", hasSize(0)));

		assertThat(stock(idInsumo)).isEqualTo(7);
		assertThat(id("SELECT COUNT(*) FROM MOVIMIENTO_KARDEX WHERE id_producto = ? AND tipo = 'SALIDA' AND id_atencion = ? AND cantidad = 3",
				idInsumo, idAtencion)).isEqualTo(1);
		assertThat(id("SELECT COUNT(*) FROM CONSUMO_PENDIENTE WHERE id_atencion = ?", idAtencion)).isZero();
	}

	@Test
	void cus11SinStockSuficienteDescuentaLoQueHayYAlmacenRegulariza() throws Exception {
		Long idInsumo = insumoConStock("Keratina de prueba", 1);
		Long idAtencion = atencionEnCursoConInsumo("Hidratación con insumo escaso", idInsumo, "3", 41);

		// RN53: descuenta 1 y deja 2 pendientes; la atención se finaliza igual
		ResultActions fin = finalizar(idAtencion).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("FINALIZADA"))
			.andExpect(jsonPath("$.consumosPendientes", hasSize(1)))
			.andExpect(jsonPath("$.consumosPendientes[0].idProducto").value(idInsumo))
			.andExpect(jsonPath("$.consumosPendientes[0].cantidadEstimada").value(3))
			.andExpect(jsonPath("$.consumosPendientes[0].cantidadDescontada").value(1))
			.andExpect(jsonPath("$.consumosPendientes[0].cantidadPendiente").value(2))
			.andExpect(jsonPath("$.consumosPendientes[0].estado").value("PENDIENTE"));
		Long idConsumo = idDe(fin, "consumosPendientes[0].idConsumo");
		assertThat(jdbc.queryForObject("SELECT estado FROM ATENCION WHERE id_atencion = ?", String.class, idAtencion)).isEqualTo("FINALIZADA");
		assertThat(stock(idInsumo)).isZero();
		assertThat(id("SELECT COUNT(*) FROM MOVIMIENTO_KARDEX WHERE id_producto = ? AND tipo = 'SALIDA' AND id_atencion = ? AND cantidad = 1",
				idInsumo, idAtencion)).isEqualTo(1);

		// CUS33: almacén lo ve en su bandeja; regularizar sin stock es 409 (RN15)
		enviar("almacen", get("/api/v1/consumos-pendientes"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.idConsumo == " + idConsumo + ")].producto", hasItem("Keratina de prueba")))
			.andExpect(jsonPath("$[?(@.idConsumo == " + idConsumo + ")].idAtencion", hasItem(idAtencion.intValue())))
			.andExpect(jsonPath("$[?(@.idConsumo == " + idConsumo + ")].fechaRegistro").exists());
		enviar("almacen", post("/api/v1/consumos-pendientes/" + idConsumo + "/regularizacion"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RN15"));

		// Con stock, la regularización es una SALIDA de la misma atención y el consumo queda REGULARIZADO
		ajustar(idInsumo, 5).andExpect(status().isOk());
		Long idMovimiento = idDe(enviar("almacen", post("/api/v1/consumos-pendientes/" + idConsumo + "/regularizacion"), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("REGULARIZADO"))
			.andExpect(jsonPath("$.idMovimiento").exists()), "idMovimiento");
		assertThat(id("SELECT COUNT(*) FROM MOVIMIENTO_KARDEX WHERE id_movimiento = ? AND tipo = 'SALIDA' AND id_atencion = ? AND cantidad = 2",
				idMovimiento, idAtencion)).isEqualTo(1);
		assertThat(stock(idInsumo)).isEqualTo(3);

		// Resuelto no cambia
		enviar("almacen", patch("/api/v1/consumos-pendientes/" + idConsumo + "/descarte"), "{\"observacion\":\"Tarde\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RN53"));
		enviar("almacen", get("/api/v1/consumos-pendientes"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idConsumo", not(hasItem(idConsumo.intValue()))));
	}

	@Test
	void cus33DescartarExigeElMotivoYCus50AvisaAAlmacen() throws Exception {
		Long idInsumo = insumoConStock("Cera de prueba", 0);
		Long idAtencion = atencionEnCursoConInsumo("Mascarilla con insumo agotado", idInsumo, "2", 42);
		Long idConsumo = idDe(finalizar(idAtencion).andExpect(status().isOk())
			.andExpect(jsonPath("$.consumosPendientes[0].cantidadDescontada").value(0)), "consumosPendientes[0].idConsumo");
		assertThat(id("SELECT COUNT(*) FROM MOVIMIENTO_KARDEX WHERE id_atencion = ?", idAtencion)).isZero();

		// CUS50: almacén ve los dos avisos; la recepcionista no ve el de consumos pendientes (no tiene KARDEX:VER)
		enviar("almacen", get("/api/v1/avisos"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].tipo", hasItem("STOCK_MINIMO")))
			.andExpect(jsonPath("$[*].tipo", hasItem("CONSUMO_PENDIENTE")))
			.andExpect(jsonPath("$[?(@.tipo == 'CONSUMO_PENDIENTE')].mensaje", hasItem(containsString("consumo"))));
		enviar("recepcion", get("/api/v1/avisos"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].tipo", not(hasItem("CONSUMO_PENDIENTE"))));

		// RN53: descartar exige la observación (422) y lo deja DESCARTADO
		enviar("almacen", patch("/api/v1/consumos-pendientes/" + idConsumo + "/descarte"), "{}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN53"));
		enviar("especialista2", patch("/api/v1/consumos-pendientes/" + idConsumo + "/descarte"), "{\"observacion\":\"Se usó menos\"}")
			.andExpect(status().isForbidden());
		enviar("almacen", patch("/api/v1/consumos-pendientes/" + idConsumo + "/descarte"), "{\"observacion\":\"Se usó menos de lo estimado\"}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("DESCARTADO"))
			.andExpect(jsonPath("$.observacion").value("Se usó menos de lo estimado"));
		assertThat(jdbc.queryForObject("SELECT estado FROM CONSUMO_PENDIENTE WHERE id_consumo = ?", String.class, idConsumo)).isEqualTo("DESCARTADO");
		enviar("almacen", post("/api/v1/consumos-pendientes/999999/regularizacion"), null).andExpect(status().isNotFound());
	}

}
