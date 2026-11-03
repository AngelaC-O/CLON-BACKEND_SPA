package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.sql.SQLException;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AuditoriaApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.comun.controller.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/** Traducción de los errores de Oracle a ProblemDetail, con la excepción anidada como la entrega Spring. */
class ApiExceptionHandlerTest {

	private final ApiExceptionHandler manejador = new ApiExceptionHandler(mock(UsuarioActual.class), mock(AuditoriaApi.class));

	private ResponseEntity<ProblemDetail> traducir(int codigo, String mensaje) {
		return manejador.inesperado(new DataIntegrityViolationException("fallo", new SQLException(mensaje, "72000", codigo)));
	}

	@Test
	void soloLaAdministradoraEs403ConLaRegla() {
		ResponseEntity<ProblemDetail> respuesta = traducir(20003, "ORA-20003: Solo la Administradora puede crear usuarios (RN39).\nORA-06512: at PKG_SESION");
		assertThat(respuesta.getStatusCode().value()).isEqualTo(403);
		assertThat(respuesta.getBody().getDetail()).isEqualTo("Solo la Administradora puede crear usuarios (RN39).");
		assertThat(respuesta.getBody().getProperties()).containsEntry("regla", "RN39");
	}

	@Test
	void sinPkgSesionEs500SinMostrarElError() {
		ResponseEntity<ProblemDetail> respuesta = traducir(20001, "ORA-20001: La conexión no identifica al usuario");
		assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
		assertThat(respuesta.getBody().getDetail()).doesNotContain("ORA-");
	}

	@Test
	void elCruceDeAgendaEs409() {
		ResponseEntity<ProblemDetail> respuesta = traducir(20010, "ORA-20010: Cruce de horario (RN02): la especialista ya tiene una cita");
		assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
		assertThat(respuesta.getBody().getProperties()).containsEntry("regla", "RN02");
	}

	@Test
	void otraReglaDeOracleEs422ConSuCodigo() {
		ResponseEntity<ProblemDetail> respuesta = traducir(20050, "ORA-20050: La venta no tiene líneas.");
		assertThat(respuesta.getStatusCode().value()).isEqualTo(422);
		assertThat(respuesta.getBody().getProperties()).containsEntry("regla", "ORA-20050");
	}

	@Test
	void unaViolacionUqEs409ConElConstraint() {
		ResponseEntity<ProblemDetail> respuesta = traducir(1, "ORA-00001: unique constraint (CAUTIVA_TEST.UQ_PERSONA_TELEFONO) violated");
		assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
		assertThat(respuesta.getBody().getDetail()).isEqualTo("El teléfono ya pertenece a otra ficha.");
		assertThat(respuesta.getBody().getProperties()).containsEntry("constraint", "UQ_PERSONA_TELEFONO");
	}

	@Test
	void unCheckEs422() {
		ResponseEntity<ProblemDetail> respuesta = traducir(2290, "ORA-02290: check constraint (CAUTIVA_TEST.CK_CAJA_ARQUEO_OBSERVACION) violated");
		assertThat(respuesta.getStatusCode().value()).isEqualTo(422);
		assertThat(respuesta.getBody().getProperties()).containsEntry("constraint", "CK_CAJA_ARQUEO_OBSERVACION");
	}

}
