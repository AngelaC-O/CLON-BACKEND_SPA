package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * [It. 2] CUS03, CUS04, CUS05 y CUS06 con lista de espera, CUS09, CUS10, CUS12, CUS58, CUS59 y CUS60, y la rama de paquete de CUS11 que en
 * la Iteración 1 no se pudo probar (D-04). La clase trabaja en su propia sede (VentasFlujoApoyo): caja, especialista y sillones con limpieza 0.
 * CUS12 usa a especialista1 y especialista2 del script 15 sobre una atención FINALIZADA en un día pasado, para no ocupar a nadie ahora.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClientasCitasCatalogoFlujoTest extends VentasFlujoApoyo {

	private static final AtomicInteger SEMANAS = new AtomicInteger(40);

	private static final AtomicInteger TELEFONOS = new AtomicInteger(987_650_000);

	private static final AtomicInteger DIAS_PASADOS = new AtomicInteger(500);

	// ------------------------------------------------------------------ apoyo

	private ResultActions como(String usuario, MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		return enviarEnSede(usuario, sedeDeLaClase(), pedido, cuerpo);
	}

	private LocalDate diaPropio() {
		return lunes(SEMANAS.incrementAndGet());
	}

	private String telefonoNuevo() {
		return String.valueOf(TELEFONOS.incrementAndGet());
	}

	private Long clientaNueva(String nombres) throws Exception {
		return idDe(enviar(post("/api/v1/clientes"),
				"{\"nombres\":\"" + nombres + "\",\"apellidos\":\"Iteración Dos\",\"telefono\":\"" + telefonoNuevo() + "\",\"aceptaComunicaciones\":true}")
			.andExpect(status().isCreated()), "idCliente");
	}

	private String cuerpoCita(Long idCliente, LocalDateTime inicio, Long idEspera) throws Exception {
		String espera = idEspera == null ? "" : ",\"idEspera\":" + idEspera;
		return "{\"idCliente\":" + idCliente + ",\"idPersonal\":" + especialista() + ",\"idRecurso\":" + recurso(0) + ",\"idVariante\":" + variante(CORTE)
				+ ",\"fechaHoraInicio\":\"" + inicio + "\",\"duracionMin\":30" + espera + "}";
	}

	private Long citaConAdelanto(Long idCliente, LocalDateTime inicio) throws Exception {
		cajaAbierta();
		Long idCita = idDe(enviar(post("/api/v1/citas"), cuerpoCita(idCliente, inicio, null)).andExpect(status().isCreated()), "idCita");
		enviar(post("/api/v1/citas/" + idCita + "/adelanto"), "{\"adelantoMonto\":20,\"idMedioPago\":" + medio("EFECTIVO") + "}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.adelantoMonto").value(20));
		return idCita;
	}

	private String estado(String tabla, String columnaId, Long id) {
		return jdbc.queryForObject("SELECT estado FROM " + tabla + " WHERE " + columnaId + " = ?", String.class, id);
	}

	private Long sedeScript15() {
		return id("SELECT p.id_sede FROM USUARIO u JOIN PERSONAL p ON p.id_personal = u.id_personal WHERE u.nombre_usuario = 'especialista1'");
	}

	private Long idServicioCorte() {
		return id("SELECT id_servicio FROM SERVICIO WHERE nombre = ?", CORTE);
	}

	private static BigDecimal decimal(ResultActions resultado, String campo) throws Exception {
		return new BigDecimal(String.valueOf((Object) JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$." + campo)));
	}

	// ------------------------------------------------------------------ CUS03 y CUS04

	@Test
	void cus03LaFichaReuneDatosCitasYSaldoYCus04ActualizaPersonaYCliente() throws Exception {
		Long idCliente = clientaNueva("Ficha");
		Long idCita = idDe(enviar(post("/api/v1/citas"), cuerpoCita(idCliente, diaPropio().atTime(10, 0), null)).andExpect(status().isCreated()), "idCita");

		enviar(get("/api/v1/clientes/" + idCliente), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.cliente.idCliente").value(idCliente))
			.andExpect(jsonPath("$.cliente.nombres").value("Ficha"))
			.andExpect(jsonPath("$.saldoFavor").value(0))
			.andExpect(jsonPath("$.citasPendientes[*].idCita", hasItem(idCita.intValue())))
			.andExpect(jsonPath("$.atenciones", hasSize(0)))
			.andExpect(jsonPath("$.paquetes", hasSize(0)))
			.andExpect(jsonPath("$.productosComprados", hasSize(0)));

		// CUS04: PERSONA por PersonaApi y CLIENTE en agenda (RF02, RN27)
		String telefono = telefonoNuevo();
		enviar(put("/api/v1/clientes/" + idCliente), "{\"nombres\":\"Ficha Editada\",\"apellidos\":\"Iteración Dos\",\"telefono\":\"" + telefono
				+ "\",\"correo\":\"ficha@correo.pe\",\"fechaCumpleanos\":\"1992-03-15\",\"observaciones\":\"Alérgica al tinte\",\"aceptaComunicaciones\":false}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombres").value("Ficha Editada"))
			.andExpect(jsonPath("$.telefono").value(telefono))
			.andExpect(jsonPath("$.correo").value("ficha@correo.pe"))
			.andExpect(jsonPath("$.fechaCumpleanos").value("1992-03-15"))
			.andExpect(jsonPath("$.observaciones").value("Alérgica al tinte"))
			.andExpect(jsonPath("$.aceptaComunicaciones").value(false));
		assertThat(id("SELECT COUNT(*) FROM AUDITORIA WHERE entidad = 'PERSONA' AND accion = 'EDITAR' AND id_entidad = ?", idCliente)).isEqualTo(1);

		// RN45: un documento verificado que cambia sin una consulta nueva queda MANUAL (TRG_PERSONA_REGLAS)
		Long idDni = id("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'");
		String dni = "7" + telefono.substring(2);
		comoDueno("UPDATE PERSONA SET id_tipo_documento = " + idDni + ", numero_documento = '" + dni
				+ "', origen_datos = 'VERIFICADO', fecha_verificacion = SYSTIMESTAMP WHERE id_persona = " + idCliente);
		String otroDni = "6" + telefono.substring(2);
		enviar(put("/api/v1/clientes/" + idCliente), "{\"nombres\":\"Ficha Editada\",\"telefono\":\"" + telefono + "\",\"idTipoDocumento\":" + idDni
				+ ",\"numeroDocumento\":\"" + otroDni + "\",\"aceptaComunicaciones\":false}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.numeroDocumento").value(otroDni))
			.andExpect(jsonPath("$.origenDatos").value("MANUAL"));
		assertThat(jdbc.queryForObject("SELECT origen_datos FROM PERSONA WHERE id_persona = ?", String.class, idCliente)).isEqualTo("MANUAL");
	}

	@Test
	void cus04UnTelefonoDeOtraClientaEs409YCus03UnaClientaInexistenteEs404() throws Exception {
		Long idCliente = clientaNueva("Repetida");
		String telefonoAjeno = jdbc.queryForObject("SELECT telefono FROM PERSONA WHERE id_persona = ?", String.class, clientaNueva("Dueña del teléfono"));

		enviar(put("/api/v1/clientes/" + idCliente), "{\"nombres\":\"Repetida\",\"telefono\":\"" + telefonoAjeno + "\",\"aceptaComunicaciones\":true}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RF05"));
		assertThat(jdbc.queryForObject("SELECT telefono FROM PERSONA WHERE id_persona = ?", String.class, idCliente)).isNotEqualTo(telefonoAjeno);

		enviar(get("/api/v1/clientes/999999"), null).andExpect(status().isNotFound());
		como("especialista1", put("/api/v1/clientes/" + idCliente), "{\"nombres\":\"X\",\"telefono\":\"987000000\",\"aceptaComunicaciones\":true}")
			.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ CUS05 con espera y CUS06

	@Test
	void cus06ListaLasEsperasDelDiaYCus05LaAsignaAlAgendar() throws Exception {
		Long idCliente = clientaNueva("En espera");
		LocalDate dia = diaPropio();
		Long idEspera = idDe(enviar(post("/api/v1/citas/lista-espera"), "{\"idCliente\":" + idCliente + ",\"idVariante\":" + variante(CORTE)
				+ ",\"fechaDeseada\":\"" + dia + "\",\"horaDesde\":\"09:00\",\"horaHasta\":\"12:00\"}").andExpect(status().isCreated()), "idEspera");

		enviar(get("/api/v1/citas/lista-espera").param("fecha", dia.toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.idEspera == " + idEspera + ")].nombreCliente", hasItem("En espera Iteración Dos")))
			.andExpect(jsonPath("$[?(@.idEspera == " + idEspera + ")].nombreServicio", hasItem(CORTE)))
			.andExpect(jsonPath("$[?(@.idEspera == " + idEspera + ")].horaDesde", hasItem("09:00")))
			.andExpect(jsonPath("$[?(@.idEspera == " + idEspera + ")].estado", hasItem("PENDIENTE")));

		Long idCita = idDe(enviar(post("/api/v1/citas"), cuerpoCita(idCliente, dia.atTime(9, 30), idEspera))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.idEspera").value(idEspera)), "idCita");

		// CK_LISTA_ESPERA_ASIGNADA: ASIGNADA con su id_cita, y ya no aparece entre las PENDIENTES
		assertThat(estado("LISTA_ESPERA", "id_espera", idEspera)).isEqualTo("ASIGNADA");
		assertThat(id("SELECT id_cita FROM LISTA_ESPERA WHERE id_espera = ?", idEspera)).isEqualTo(idCita);
		enviar(get("/api/v1/citas/lista-espera").param("fecha", dia.toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idEspera", not(hasItem(idEspera.intValue()))));
	}

	@Test
	void cus06DescartaLaEsperaYCus05DesdeUnaEsperaDescartadaEs409() throws Exception {
		Long idCliente = clientaNueva("Descartada");
		LocalDate dia = diaPropio();
		Long idEspera = idDe(enviar(post("/api/v1/citas/lista-espera"), "{\"idCliente\":" + idCliente + ",\"idVariante\":" + variante(CORTE)
				+ ",\"fechaDeseada\":\"" + dia + "\"}").andExpect(status().isCreated()), "idEspera");

		enviar(patch("/api/v1/citas/lista-espera/" + idEspera + "/descarte"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("DESCARTADA"));
		enviar(patch("/api/v1/citas/lista-espera/" + idEspera + "/descarte"), null).andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RF09"));

		// La cita no se guarda: misma transacción que la espera
		long citasAntes = id("SELECT COUNT(*) FROM CITA WHERE id_cliente = ?", idCliente);
		enviar(post("/api/v1/citas"), cuerpoCita(idCliente, dia.atTime(11, 0), idEspera)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.regla").value("RF09"));
		assertThat(id("SELECT COUNT(*) FROM CITA WHERE id_cliente = ?", idCliente)).isEqualTo(citasAntes);

		enviar(patch("/api/v1/citas/lista-espera/999999/descarte"), null).andExpect(status().isNotFound());
	}

	// ------------------------------------------------------------------ CUS09

	@Test
	void cus09ReprogramaConElAdelantoYAlPasadoEs422() throws Exception {
		Long idCliente = clientaNueva("Reprogramada");
		LocalDate dia = diaPropio();
		Long idCita = citaConAdelanto(idCliente, dia.atTime(10, 0));

		Long idNueva = idDe(enviar(post("/api/v1/citas/" + idCita + "/reprogramacion"), "{\"fechaHoraInicio\":\"" + dia.atTime(15, 0) + "\",\"idPersonal\":"
				+ especialista() + ",\"idRecurso\":" + recurso(1) + "}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("PENDIENTE"))
			.andExpect(jsonPath("$.idCitaOrigen").value(idCita))
			.andExpect(jsonPath("$.adelantoMonto").value(20))
			.andExpect(jsonPath("$.duracionMin").value(30)), "idCita");

		assertThat(estado("CITA", "id_cita", idCita)).isEqualTo("REPROGRAMADA");
		assertThat(jdbc.queryForObject("SELECT adelanto_monto FROM CITA WHERE id_cita = ?", BigDecimal.class, idNueva)).isEqualByComparingTo("20");

		// RN49: no se reprograma a una fecha y hora que ya pasaron; una cita ya reprogramada no se vuelve a reprogramar
		enviar(post("/api/v1/citas/" + idNueva + "/reprogramacion"), "{\"fechaHoraInicio\":\"" + LocalDateTime.now().minusDays(1).withNano(0)
				+ "\",\"idPersonal\":" + especialista() + ",\"idRecurso\":" + recurso(1) + "}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN49"));
		enviar(post("/api/v1/citas/" + idCita + "/reprogramacion"), "{\"fechaHoraInicio\":\"" + dia.atTime(17, 0) + "\",\"idPersonal\":" + especialista()
				+ ",\"idRecurso\":" + recurso(1) + "}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF12"));
		assertThat(estado("CITA", "id_cita", idNueva)).isEqualTo("PENDIENTE");
	}

	// ------------------------------------------------------------------ CUS10

	@Test
	void cus10CancelarConAdelantoDejaUnAbonoEnSaldoAFavor() throws Exception {
		Long idCliente = clientaNueva("Cancelada");
		Long idCita = citaConAdelanto(idCliente, diaPropio().atTime(10, 0));

		enviar(patch("/api/v1/citas/" + idCita + "/cancelacion"), "{\"motivoCancelacion\":\"Viaje imprevisto\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CANCELADA"))
			.andExpect(jsonPath("$.motivoCancelacion").value("Viaje imprevisto"));

		assertThat(jdbc.queryForObject("SELECT monto FROM SALDO_FAVOR_MOV WHERE tipo = 'ABONO' AND id_cita = ? AND id_cliente = ?", BigDecimal.class,
				idCita, idCliente)).isEqualByComparingTo("20");
		enviar(get("/api/v1/clientes/" + idCliente), null).andExpect(status().isOk()).andExpect(jsonPath("$.saldoFavor").value(20));

		// Sin motivo es 422 (CK_CITA_CANCELACION); una cita cancelada no se cancela otra vez
		Long otra = citaConAdelanto(idCliente, diaPropio().atTime(11, 0));
		enviar(patch("/api/v1/citas/" + otra + "/cancelacion"), "{\"motivoCancelacion\":\" \"}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF12"));
		enviar(patch("/api/v1/citas/" + idCita + "/cancelacion"), "{\"motivoCancelacion\":\"Otra vez\"}").andExpect(status().isUnprocessableContent());
		assertThat(id("SELECT COUNT(*) FROM SALDO_FAVOR_MOV WHERE id_cita = ?", idCita)).isEqualTo(1);
	}

	@Test
	void cus10LaInasistenciaEsUnaCancelacionConElMismoAbono() throws Exception {
		Long idCliente = clientaNueva("Ausente");
		Long idCita = citaConAdelanto(idCliente, diaPropio().atTime(10, 0));

		enviar(patch("/api/v1/citas/" + idCita + "/cancelacion"), "{\"motivoCancelacion\":\"No se presentó\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("CANCELADA"))
			.andExpect(jsonPath("$.motivoCancelacion").value("No se presentó"));

		assertThat(jdbc.queryForObject("SELECT monto FROM SALDO_FAVOR_MOV WHERE tipo = 'ABONO' AND id_cita = ?", BigDecimal.class, idCita))
			.isEqualByComparingTo("20");
		assertThat(id("SELECT NVL(MAX(saldo), 0) FROM V_SALDO_FAVOR_CLIENTE WHERE id_cliente = ?", idCliente)).isEqualTo(20L);
	}

	// ------------------------------------------------------------------ CUS12

	@Test
	void cus12LaEspecialistaRecomiendaUnProductoYOtraEspecialistaRecibe403() throws Exception {
		Long idCliente = clientaNueva("Recomendada");
		Long idEspecialista1 = id("SELECT id_personal FROM USUARIO WHERE nombre_usuario = 'especialista1'");
		Long idRecurso = id("SELECT MIN(id_recurso) FROM RECURSO WHERE id_sede = ? AND tipo = 'SILLON'", sedeScript15());
		comoDueno("INSERT INTO ATENCION (id_cliente, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES (" + idCliente + ", "
				+ idEspecialista1 + ", " + idRecurso + ", " + variante(CORTE) + ", CAST(SYSTIMESTAMP AS TIMESTAMP) - " + DIAS_PASADOS.incrementAndGet() + ", 30)");
		Long idAtencion = id("SELECT MAX(id_atencion) FROM ATENCION WHERE id_cliente = ?", idCliente);
		comoDueno("UPDATE ATENCION SET estado = 'FINALIZADA', hora_fin = hora_inicio + INTERVAL '30' MINUTE WHERE id_atencion = " + idAtencion);
		Long idProducto = id("SELECT MIN(id_producto) FROM PRODUCTO WHERE tipo_uso = 'VENTA' AND estado = 'ACTIVO'");
		String nombreProducto = jdbc.queryForObject("SELECT nombre FROM PRODUCTO WHERE id_producto = ?", String.class, idProducto);

		enviarEnSede("especialista1", sedeScript15(), get("/api/v1/atenciones/productos"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", hasItem(nombreProducto)));

		String cuerpo = "{\"idProducto\":" + idProducto + ",\"observacion\":\"Para mantener el color\"}";
		enviarEnSede("especialista2", sedeScript15(), post("/api/v1/atenciones/" + idAtencion + "/recomendaciones"), cuerpo)
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.regla").value("RN50"));

		Long idRecomendacion = idDe(enviarEnSede("especialista1", sedeScript15(), post("/api/v1/atenciones/" + idAtencion + "/recomendaciones"), cuerpo)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.idAtencion").value(idAtencion))
			.andExpect(jsonPath("$.idProducto").value(idProducto))
			.andExpect(jsonPath("$.fecha").exists()), "idRecomendacion");
		assertThat(id("SELECT id_atencion FROM RECOMENDACION_PRODUCTO WHERE id_recomendacion = ?", idRecomendacion)).isEqualTo(idAtencion);

		// UQ_RECOM_ATENCION_PRODUCTO: el mismo producto una vez por atención
		enviarEnSede("especialista1", sedeScript15(), post("/api/v1/atenciones/" + idAtencion + "/recomendaciones"), cuerpo)
			.andExpect(status().isConflict());

		// La recomendación aparece en la ficha de la clienta (CUS03)
		enviar(get("/api/v1/clientes/" + idCliente), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.atenciones[0].idAtencion").value(idAtencion))
			.andExpect(jsonPath("$.atenciones[0].recomendaciones[0].idProducto").value(idProducto));
	}

	// ------------------------------------------------------------------ CUS58

	@Test
	void cus58ActualizaDesactivaYCambiaElPrecioQueAuditaOracleYLaEspecialistaRecibe403() throws Exception {
		Long idArea = id("SELECT id_area FROM AREA WHERE nombre LIKE 'Peluquer%'");
		String nombre = "Ondulado It2 " + SEMANAS.incrementAndGet();
		ResultActions creado = como("admin", post("/api/v1/servicios"), "{\"idArea\":" + idArea + ",\"nombre\":\"" + nombre
				+ "\",\"duracionRefMin\":60,\"tipoRecurso\":\"SILLON\",\"variantes\":[{\"nombreVariante\":\"Corto\",\"precio\":80}]}")
			.andExpect(status().isCreated());
		Long idServicio = idDe(creado, "idServicio");
		Long idVariante = idDe(creado, "variantes[0].idVariante");

		como("recepcion", get("/api/v1/servicios").param("estado", "ACTIVO"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].nombre", hasItem(CORTE)))
			.andExpect(jsonPath("$[?(@.idServicio == " + idServicio + ")].variantes[0].idVariante", hasItem(idVariante.intValue())));

		// Insumo en su unidad mínima (RN54) que crea almacén
		Long idInsumo = idDe(enviarEnSede("almacen", sedeScript15(), post("/api/v1/productos"), "{\"codigo\":\"IT2-" + SEMANAS.incrementAndGet()
				+ "\",\"nombre\":\"Loción It2\",\"idCategoria\":" + id("SELECT id_categoria FROM CATEGORIA_PRODUCTO WHERE nombre = 'Insumos de servicio'")
				+ ",\"unidad\":\"ml\",\"tipoUso\":\"INSUMO\",\"costo\":1.50,\"stockMinimo\":1}").andExpect(status().isCreated()), "idProducto");

		como("admin", put("/api/v1/servicios/" + idServicio), "{\"idArea\":" + idArea + ",\"nombre\":\"" + nombre + " editado\",\"duracionRefMin\":45,"
				+ "\"tipoRecurso\":\"SILLON\",\"estado\":\"INACTIVO\",\"variantes\":[{\"idVariante\":" + idVariante + ",\"nombreVariante\":\"Corto\",\"precio\":80,"
				+ "\"insumos\":[{\"idProducto\":" + idInsumo + ",\"cantidadEstimada\":15}]}]}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value(nombre + " editado"))
			.andExpect(jsonPath("$.duracionRefMin").value(45))
			.andExpect(jsonPath("$.estado").value("INACTIVO"))
			.andExpect(jsonPath("$.variantes[0].insumos[0].idProducto").value(idInsumo));
		assertThat(estado("SERVICIO", "id_servicio", idServicio)).isEqualTo("INACTIVO");
		assertThat(jdbc.queryForObject("SELECT cantidad_estimada FROM SERVICIO_INSUMO WHERE id_variante = ? AND id_producto = ?", BigDecimal.class,
				idVariante, idInsumo)).isEqualByComparingTo("15");

		// RN25: solo la Administradora cambia precios y TRG_AUD_VARIANTE_SERVICIO lo registra
		long auditoriasAntes = id("SELECT COUNT(*) FROM AUDITORIA WHERE entidad = 'VARIANTE_SERVICIO' AND id_entidad = ?", idVariante);
		como("admin", patch("/api/v1/servicios/" + idServicio + "/variantes/" + idVariante + "/precio"), "{\"precioNuevo\":95}").andExpect(status().isOk())
			.andExpect(jsonPath("$.variantes[0].precio").value(95));
		assertThat(jdbc.queryForObject("SELECT precio FROM VARIANTE_SERVICIO WHERE id_variante = ?", BigDecimal.class, idVariante)).isEqualByComparingTo("95");
		assertThat(id("SELECT COUNT(*) FROM AUDITORIA WHERE entidad = 'VARIANTE_SERVICIO' AND id_entidad = ?", idVariante)).isGreaterThan(auditoriasAntes);

		enviarEnSede("especialista1", sedeScript15(), patch("/api/v1/servicios/" + idServicio + "/variantes/" + idVariante + "/precio"), "{\"precioNuevo\":1}")
			.andExpect(status().isForbidden());
		como("admin", patch("/api/v1/servicios/" + idServicio + "/variantes/" + idVariante + "/precio"), "{\"precioNuevo\":-1}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RN40"));
		assertThat(jdbc.queryForObject("SELECT precio FROM VARIANTE_SERVICIO WHERE id_variante = ?", BigDecimal.class, idVariante)).isEqualByComparingTo("95");
	}

	// ------------------------------------------------------------------ CUS60

	@Test
	void cus60RegistraPromocionesYSoloListaLasVigentesHoy() throws Exception {
		LocalDate hoy = LocalDate.now();
		Long vigente = idDe(como("admin", post("/api/v1/promociones"), "{\"nombre\":\"Vigente It2\",\"tipoDescuento\":\"PORCENTAJE\",\"valor\":10,"
				+ "\"fechaInicio\":\"" + hoy.minusDays(1) + "\",\"fechaFin\":\"" + hoy.plusDays(5) + "\",\"idsServicio\":[" + idServicioCorte() + "]}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("ACTIVA"))
			.andExpect(jsonPath("$.idsServicio[0]").value(idServicioCorte())), "idPromocion");
		Long futura = idDe(como("admin", post("/api/v1/promociones"), "{\"nombre\":\"Futura It2\",\"tipoDescuento\":\"MONTO\",\"valor\":5,"
				+ "\"fechaInicio\":\"" + hoy.plusDays(10) + "\",\"fechaFin\":\"" + hoy.plusDays(20) + "\"}").andExpect(status().isCreated()), "idPromocion");
		assertThat(id("SELECT COUNT(*) FROM PROMOCION_SERVICIO WHERE id_promocion = ?", vigente)).isEqualTo(1);

		como("recepcion", get("/api/v1/promociones").param("vigentes", "true"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idPromocion", hasItem(vigente.intValue())))
			.andExpect(jsonPath("$[*].idPromocion", not(hasItem(futura.intValue()))));
		como("recepcion", get("/api/v1/promociones"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idPromocion", hasItem(futura.intValue())));

		// CK_PROMOCION_VALOR: un porcentaje sobre 100 es 422; la recepcionista no crea promociones
		como("admin", post("/api/v1/promociones"), "{\"nombre\":\"Imposible\",\"tipoDescuento\":\"PORCENTAJE\",\"valor\":150,\"fechaInicio\":\"" + hoy
				+ "\",\"fechaFin\":\"" + hoy + "\"}").andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RF84"));
		como("recepcion", post("/api/v1/promociones"), "{\"nombre\":\"Ajena\",\"tipoDescuento\":\"MONTO\",\"valor\":5,\"fechaInicio\":\"" + hoy
				+ "\",\"fechaFin\":\"" + hoy + "\"}").andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ CUS59 y CUS11 con paquete (D-04)

	@Test
	void cus59RegistraElPaqueteYCus11DescuentaLaSesionDeLaClienta() throws Exception {
		Long idPaquete = idDe(como("admin", post("/api/v1/paquetes"), "{\"nombre\":\"Cortes It2 " + SEMANAS.incrementAndGet()
				+ "\",\"tipo\":\"PAQUETE\",\"numSesiones\":3,\"precio\":60,\"vigenciaDias\":90,\"idsServicio\":[" + idServicioCorte() + "]}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.estado").value("ACTIVO"))
			.andExpect(jsonPath("$.idsServicio[0]").value(idServicioCorte())), "idPaquete");
		assertThat(id("SELECT COUNT(*) FROM PAQUETE_SERVICIO WHERE id_paquete = ? AND id_servicio = ?", idPaquete, idServicioCorte())).isEqualTo(1);
		como("recepcion", get("/api/v1/paquetes").param("tipo", "PAQUETE"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].idPaquete", hasItem(idPaquete.intValue())));
		como("recepcion", post("/api/v1/paquetes"), "{\"nombre\":\"Ajeno\",\"tipo\":\"PAQUETE\",\"numSesiones\":1,\"precio\":1}")
			.andExpect(status().isForbidden());
		como("admin", post("/api/v1/paquetes"), "{\"nombre\":\"Sin sesiones\",\"tipo\":\"PAQUETE\",\"numSesiones\":0,\"precio\":1}")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.regla").value("RF84"));

		// La clienta compra el paquete y un producto en una venta cerrada. La venta de paquetes y productos (CUS16, CUS26) no es de esta
		// tarea: el dueño agrega las líneas a la venta ABIERTA y el cierre de ventas registra CLIENTE_PAQUETE (PaqueteClienteApi).
		Long idCliente = clientaNueva("Con paquete");
		cajaAbierta();
		Long idVenta = idDe(crearVenta(atencionFinalizada(idCliente, variante(CORTE))).andExpect(status().isCreated()), "idVenta");
		Long idProducto = id("SELECT MIN(id_producto) FROM PRODUCTO WHERE tipo_uso = 'VENTA' AND estado = 'ACTIVO' AND precio_venta IS NOT NULL");
		Long idAdmin = id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'admin'");
		if (id("SELECT COUNT(*) FROM INVENTARIO_SEDE WHERE id_producto = ? AND id_sede = ?", idProducto, sedeDeLaClase()) == 0) {
			comoDueno("INSERT INTO INVENTARIO_SEDE (id_producto, id_sede) VALUES (" + idProducto + ", " + sedeDeLaClase() + ")");
		}
		comoDueno("INSERT INTO MOVIMIENTO_KARDEX (id_producto, id_sede, tipo, motivo, cantidad, saldo, id_usuario) VALUES (" + idProducto + ", "
				+ sedeDeLaClase() + ", 'ENTRADA', 'Stock de prueba', 5, 0, " + idAdmin + ")");
		comoDueno("INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_paquete, cantidad, precio_unitario, descuento, subtotal) VALUES (" + idVenta
				+ ", 'PAQUETE', " + idPaquete + ", 1, 0, 0, 0)");
		comoDueno("INSERT INTO VENTA_DETALLE (id_venta, tipo_item, id_producto, cantidad, precio_unitario, descuento, subtotal) VALUES (" + idVenta
				+ ", 'PRODUCTO', " + idProducto + ", 1, 0, 0, 0)");
		BigDecimal total = decimal(enviar(patch("/api/v1/ventas/" + idVenta + "/descuentos"), "{}").andExpect(status().isOk()), "total");
		pagar(idVenta, medio("EFECTIVO"), total.toPlainString(), null).andExpect(status().isOk());
		cerrarConBoleta(idVenta).andExpect(status().isOk());
		Long idClientePaquete = id("SELECT id_cliente_paquete FROM CLIENTE_PAQUETE WHERE id_cliente = ? AND id_paquete = ?", idCliente, idPaquete);

		// CUS11: la atención de un Corte de la clienta usa una sesión del paquete (RN36); TRG_ATENCION_PAQUETE lo repite
		Long idCita = idDe(enviar(post("/api/v1/citas"), cuerpoCita(idCliente, diaPropio().atTime(10, 0), null)).andExpect(status().isCreated()), "idCita");
		Long idAtencion = idDe(como("admin", post("/api/v1/atenciones"), "{\"idCita\":" + idCita + ",\"tiempoDeclaradoMin\":30}")
			.andExpect(status().isCreated()), "idAtencion");
		como("admin", patch("/api/v1/atenciones/" + idAtencion + "/fin"), "{\"observaciones\":\"Con paquete\"}").andExpect(status().isOk())
			.andExpect(jsonPath("$.estado").value("FINALIZADA"))
			.andExpect(jsonPath("$.idClientePaquete").value(idClientePaquete));
		assertThat(id("SELECT sesiones_usadas FROM V_SESIONES_PAQUETE WHERE id_cliente_paquete = ?", idClientePaquete)).isEqualTo(1L);

		// CUS03: la ficha muestra el paquete con sus sesiones y el producto comprado (HistorialComprasProvider de ventas)
		enviar(get("/api/v1/clientes/" + idCliente), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.paquetes[0].idClientePaquete").value(idClientePaquete))
			.andExpect(jsonPath("$.paquetes[0].sesionesTotales").value(3))
			.andExpect(jsonPath("$.paquetes[0].sesionesUsadas").value(1))
			.andExpect(jsonPath("$.paquetes[0].sesionesRestantes").value(2))
			.andExpect(jsonPath("$.productosComprados[0].idVenta").value(idVenta))
			.andExpect(jsonPath("$.productosComprados[0].idProducto").value(idProducto))
			.andExpect(jsonPath("$.productosComprados[0].cantidad").value(1))
			.andExpect(jsonPath("$.atenciones[*].idClientePaquete", hasItem(idClientePaquete.intValue())));
	}

}
