package dev.com.Synapsis_Spa_Cautiva.comun.controller;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AuditoriaApi;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Todos los errores salen como ProblemDetail (RFC 9457); la propiedad "regla" lleva la RN, el RF o el código de Oracle. */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final int APLICACION_MIN = 20000;
	private static final int APLICACION_MAX = 20999;
	private static final int SIN_SESION_ORACLE = 20001;
	private static final int SOLO_ADMINISTRADORA = 20003;
	private static final int CRUCE_CITA = 20010;
	private static final int CRUCE_ATENCION = 20011;
	private static final int UNICA = 1;
	private static final int CHECK = 2290;
	private static final int PADRE_NO_EXISTE = 2291;
	private static final int TIENE_HIJOS = 2292;

	private static final Pattern MENSAJE_APLICACION = Pattern.compile("ORA-\\d+:\\s*([^\\r\\n]*)");
	private static final Pattern REGLA = Pattern.compile("\\b(RN\\d+|RF\\d+)\\b");
	private static final Pattern CONSTRAINT = Pattern.compile("\\([\\w$#]+\\.([\\w$#]+)\\)");

	private static final Map<String, String> MENSAJES = Map.of(
			"UQ_PERSONA_TELEFONO", "El teléfono ya pertenece a otra ficha.",
			"UQ_PERSONA_DOCUMENTO", "El documento ya pertenece a otra ficha.",
			"UQ_USUARIO_NOMBRE_USUARIO", "Ese nombre de usuario ya existe.",
			"UQ_USUARIO_ID_PERSONAL", "Esa persona del personal ya tiene un usuario.",
			"UQ_AREA_NOMBRE", "Ya existe un área con ese nombre.",
			"UQ_SERVICIO_ID_AREA_NOMBRE", "El área ya tiene un servicio con ese nombre.",
			"UQ_RECURSO_ID_SEDE_NOMBRE", "La sede ya tiene un recurso con ese nombre.",
			"UQ_CAJA_ABIERTA_SEDE", "La sede ya tiene una caja abierta (RN30).",
			"CK_CAJA_ARQUEO_OBSERVACION", "Toda diferencia del arqueo lleva una observación (RN30).");

	private final UsuarioActual usuarioActual;

	private final AuditoriaApi auditoriaApi;

	@ExceptionHandler(ReglaNegocioException.class)
	public ResponseEntity<ProblemDetail> reglaNegocio(ReglaNegocioException ex) {
		return respuesta(ex.getEstado(), ex.getMessage(), ex.getRegla(), null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> accesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
		auditarAccesoDenegado(request);
		return respuesta(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación.", "ACCESO_DENEGADO", null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ProblemDetail> sinSesion(AuthenticationException ex) {
		return respuesta(HttpStatus.UNAUTHORIZED, "Inicie sesión para continuar.", "SIN_SESION", null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> inesperado(Exception ex) {
		return buscarErrorOracle(ex).map(this::traducirOracle).orElseGet(() -> errorInterno(ex));
	}

	private ResponseEntity<ProblemDetail> traducirOracle(SQLException ex) {
		int codigo = ex.getErrorCode();
		String mensaje = ex.getMessage() == null ? "" : ex.getMessage();
		if (codigo >= APLICACION_MIN && codigo <= APLICACION_MAX) {
			return traducirAplicacion(codigo, mensaje, ex);
		}
		String constraint = extraer(CONSTRAINT, mensaje);
		return switch (codigo) {
			case UNICA -> conConstraint(HttpStatus.CONFLICT, constraint, "Ya existe un registro con ese dato.");
			case CHECK -> conConstraint(HttpStatus.UNPROCESSABLE_CONTENT, constraint, "El dato no cumple una regla de la base de datos.");
			case PADRE_NO_EXISTE -> conConstraint(HttpStatus.UNPROCESSABLE_CONTENT, constraint, "El dato relacionado no existe.");
			case TIENE_HIJOS -> conConstraint(HttpStatus.CONFLICT, constraint, "El registro está en uso y no se puede eliminar.");
			default -> errorInterno(ex);
		};
	}

	private ResponseEntity<ProblemDetail> traducirAplicacion(int codigo, String mensaje, SQLException ex) {
		if (codigo == SIN_SESION_ORACLE) {
			return errorInterno(ex);
		}
		HttpStatus estado = switch (codigo) {
			case SOLO_ADMINISTRADORA -> HttpStatus.FORBIDDEN;
			case CRUCE_CITA, CRUCE_ATENCION -> HttpStatus.CONFLICT;
			default -> HttpStatus.UNPROCESSABLE_CONTENT;
		};
		String detalle = extraer(MENSAJE_APLICACION, mensaje);
		String regla = extraer(REGLA, mensaje);
		return respuesta(estado, detalle, regla != null ? regla : "ORA-" + codigo, null);
	}

	private ResponseEntity<ProblemDetail> conConstraint(HttpStatus estado, String constraint, String mensajePorDefecto) {
		String detalle = constraint == null ? mensajePorDefecto : MENSAJES.getOrDefault(constraint, mensajePorDefecto);
		return respuesta(estado, detalle, constraint, constraint);
	}

	private ResponseEntity<ProblemDetail> errorInterno(Exception ex) {
		log.error("Error no controlado", ex);
		return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno.", "ERROR_INTERNO", null);
	}

	private ResponseEntity<ProblemDetail> respuesta(HttpStatus estado, String detalle, String regla, String constraint) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setProperty("regla", regla);
		if (constraint != null) {
			problema.setProperty("constraint", constraint);
		}
		return ResponseEntity.status(estado).body(problema);
	}

	/** Si falla, la auditoría no cambia la respuesta: el usuario igual recibe su 403. */
	private void auditarAccesoDenegado(HttpServletRequest request) {
		try {
			usuarioActual.buscarIdUsuario().ifPresent(idUsuario -> auditoriaApi.registrar(idUsuario, "ACCESO_DENEGADO", "ENDPOINT", null,
					request.getMethod() + " " + request.getRequestURI()));
		}
		catch (RuntimeException ex) {
			log.error("No se pudo auditar el acceso denegado", ex);
		}
	}

	private static Optional<SQLException> buscarErrorOracle(Throwable ex) {
		for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
			if (causa instanceof SQLException sql && sql.getErrorCode() != 0) {
				return Optional.of(sql);
			}
		}
		return Optional.empty();
	}

	private static String extraer(Pattern patron, String texto) {
		Matcher coincidencia = patron.matcher(texto);
		return coincidencia.find() ? coincidencia.group(1) : null;
	}

}
