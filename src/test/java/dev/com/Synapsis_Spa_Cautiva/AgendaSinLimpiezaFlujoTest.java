package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * [It. 2.1 · RN03 v5.2] Sin tiempo de limpieza: la especialista y el recurso quedan libres a la misma hora, al terminar el servicio, en Java
 * (GestorDisponibilidadAgenda) y en Oracle (PKG_AGENDA, TRG_CITA_CRUCE, TRG_ATENCION_CRUCE). La clase trabaja en su propia sede
 * (VentasFlujoApoyo), con su especialista y una segunda especialista de Peluquería que agrega aquí, para no cruzarse con otras pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AgendaSinLimpiezaFlujoTest extends VentasFlujoApoyo {

	private static final AtomicInteger SEMANAS = new AtomicInteger(60);

	private static final AtomicInteger NUMERO = new AtomicInteger(0);

	private Long otraEspecialista;

	private Long otraEspecialista() throws SQLException {
		if (otraEspecialista == null) {
			Long idSede = sedeDeLaClase();
			int numero = (int) (idSede % 1000) * 10 + NUMERO.incrementAndGet();
			String dni = String.format("7299%04d", numero);
			String telefono = String.format("98799%04d", numero);
			comoDueno("INSERT INTO PERSONA (tipo_persona, nombres, apellidos, id_tipo_documento, numero_documento, telefono) "
					+ "SELECT 'NATURAL', 'Otra', 'Especialista', id_tipo_documento, '" + dni + "', '" + telefono + "' FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'");
			otraEspecialista = id("SELECT id_persona FROM PERSONA WHERE numero_documento = ?", dni);
			comoDueno("INSERT INTO PERSONAL (id_personal, id_sede, cargo, modalidad_pago, sueldo_base, porcentaje_comision, fecha_ingreso) VALUES ("
					+ otraEspecialista + ", " + idSede + ", 'ESPECIALISTA_TECNICA', 'COMISION', 0, 30, TRUNC(SYSDATE))");
			for (int dia = 1; dia <= 7; dia++) {
				comoDueno("INSERT INTO HORARIO_PERSONAL (id_personal, dia_semana, hora_entrada, hora_salida, minutos_almuerzo) VALUES ("
						+ otraEspecialista + ", " + dia + ", '00:00', '23:59', 0)");
			}
			comoDueno("INSERT INTO PERSONAL_AREA (id_personal, id_area) SELECT " + otraEspecialista + ", id_area FROM AREA WHERE nombre LIKE 'Peluquer%'");
		}
		return otraEspecialista;
	}

	private ResultActions agendar(Long idCliente, Long idPersonal, Long idRecurso, LocalDateTime inicio) throws Exception {
		return enviar(post("/api/v1/citas"), "{\"idCliente\":" + idCliente + ",\"idPersonal\":" + idPersonal + ",\"idRecurso\":" + idRecurso
				+ ",\"idVariante\":" + variante(CORTE) + ",\"fechaHoraInicio\":\"" + inicio + "\",\"duracionMin\":30}");
	}

	@Test
	void rn03LaEspecialistaYElRecursoQuedanLibresALaHoraDeFin() throws Exception {
		Long idCliente = clienta("Hora de fin");
		LocalDate dia = lunes(SEMANAS.incrementAndGet());
		agendar(idCliente, especialista(), recurso(0), dia.atTime(10, 0)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.fechaHoraFin").value(containsString("10:30")));

		// Dentro del servicio, Java responde 409 por la especialista y por el recurso
		agendar(idCliente, especialista(), recurso(1), dia.atTime(10, 15)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("especialista")));
		agendar(idCliente, otraEspecialista(), recurso(0), dia.atTime(10, 15)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("recurso")));

		// Las horas libres ofrecen a los dos a las 10:30 (en v5.1 el sillón seguía en limpieza hasta las 10:40)
		enviar(get("/api/v1/citas/disponibilidad").param("idVariante", variante(CORTE).toString()).param("fecha", dia.toString()), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.inicio == '" + dia.atTime(10, 30) + ":00')].idsPersonal[*]", hasItem(especialista().intValue())))
			.andExpect(jsonPath("$[?(@.inicio == '" + dia.atTime(10, 30) + ":00')].idsRecurso[*]", hasItem(recurso(0).intValue())));

		// A la hora de fin, Java y Oracle aceptan a la misma especialista en otro sillón y al mismo sillón con otra especialista
		agendar(idCliente, especialista(), recurso(1), dia.atTime(10, 30)).andExpect(status().isCreated());
		agendar(idCliente, otraEspecialista(), recurso(0), dia.atTime(10, 30)).andExpect(status().isCreated());
	}

	@Test
	void rn03UnaAtencionQueTerminaAntesLiberaDesdeSuHoraFinEnJavaYEnOracle() throws Exception {
		// Atención sin cita de la especialista en el sillón 2: declaró 60 minutos y termina ahora, a los pocos segundos
		comoDueno("INSERT INTO ATENCION (nombre_cliente_paso, id_personal, id_recurso, id_variante, hora_inicio, tiempo_declarado_min) VALUES ("
				+ "'Termina antes', " + especialista() + ", " + recurso(2) + ", " + variante(CORTE) + ", CAST(SYSTIMESTAMP AS TIMESTAMP) - INTERVAL '2' SECOND, 60)");
		Long idAtencion = id("SELECT MAX(id_atencion) FROM ATENCION WHERE nombre_cliente_paso = 'Termina antes' AND id_recurso = ?", recurso(2));
		comoDueno("UPDATE ATENCION SET estado = 'FINALIZADA' WHERE id_atencion = " + idAtencion);
		assertThat(id("SELECT COUNT(*) FROM ATENCION WHERE id_atencion = ? AND hora_fin < hora_inicio + INTERVAL '60' MINUTE", idAtencion)).isEqualTo(1);

		// Java: la venta rápida (CUS13) ofrece a la especialista y al sillón 2 desde hora_fin, no desde inicio + 60
		enviar(get("/api/v1/ventas/rapidas/libres").param("idVariante", variante(CORTE).toString()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].idsPersonal", hasItem(especialista().intValue())))
			.andExpect(jsonPath("$[0].idsRecurso", hasItem(recurso(2).intValue())));

		// Java (validarSinCruce) y Oracle (validar_atencion, sin -20011) dejan iniciar otra atención suya en el mismo sillón (CUS11)
		Long idCliente = clienta("Después de hora_fin");
		Long idCita = idDe(agendar(idCliente, especialista(), recurso(2), lunes(SEMANAS.incrementAndGet()).atTime(11, 0)).andExpect(status().isCreated()),
				"idCita");
		Long idSiguiente = idDe(enviarEnSede("admin", sedeDeLaClase(), post("/api/v1/atenciones"), "{\"idCita\":" + idCita + ",\"tiempoDeclaradoMin\":30}")
			.andExpect(status().isCreated()), "idAtencion");
		enviarEnSede("admin", sedeDeLaClase(), patch("/api/v1/atenciones/" + idSiguiente + "/fin"), "{\"observaciones\":\"Sin novedad\"}")
			.andExpect(status().isOk());
	}

}
