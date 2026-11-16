package dev.com.Synapsis_Spa_Cautiva.comun.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Completa lo que springdoc saca del código con openapi/contrato.yaml: resumen, CUS, estados y ejemplos de cada operación (U2 §5). De aquí sale
 * docs/openapi.yaml; ContratoOpenApiTest comprueba que los dos sigan iguales. [It. 2] Cada operación lleva x-permiso (Duda 1 del front-end): el
 * MODULO:ACCION de su @PreAuthorize, SESION si basta con iniciar sesión o PUBLICO. El 403 trae sus ejemplos: sin permiso (ACCESO_DENEGADO), con
 * la clave temporal sin cambiar (RF82, FiltroCambioClave; diferencia 20 del front-end) y, si la operación lo tiene, el de su regla.
 */
@Configuration
class ContratoOpenApi {

	private static final String RUTA_CONTRATO = "openapi/contrato.yaml";

	private static final String JSON = "application/json";

	private static final String PROBLEMA = "application/problem+json";

	private static final String SESION = "sesion";

	private static final String CSRF = "csrf";

	private static final String ESQUEMA_PROBLEMA = "ProblemDetail";

	private static final String X_PERMISO = "x-permiso";

	private static final String PERMISO_SESION = "SESION";

	private static final String PERMISO_PUBLICO = "PUBLICO";

	private static final Pattern AUTORIDAD = Pattern.compile("hasAuthority\\('([A-Z_]+:[A-Z_]+)'\\)");

	/** Las que FiltroCambioClave deja pasar con la clave temporal: su 403 no es RF82. */
	private static final Set<String> SIN_CAMBIO_DE_CLAVE = Set.of("/api/v1/auth/cambio-clave", "/api/v1/auth/logout", "/api/v1/auth/sesion");

	/** [It. 2] x-permiso desde el @PreAuthorize del método (Duda 1 del front-end). */
	@Bean
	OperationCustomizer permisoDeCadaOperacion() {
		return (oOperation, metodo) -> {
			PreAuthorize oPreAuthorize = metodo.getMethodAnnotation(PreAuthorize.class);
			if (oPreAuthorize != null) {
				Matcher autoridad = AUTORIDAD.matcher(oPreAuthorize.value());
				oOperation.addExtension(X_PERMISO, autoridad.find() ? autoridad.group(1) : PERMISO_SESION);
			}
			return oOperation;
		};
	}

	@Bean
	OpenApiCustomizer contratoDeLaIteracion() {
		Map<String, Object> contrato = leerContrato();
		return openApi -> {
			openApi.info(new Info().title("Cautiva Spa · BackOffice · Iteración 2.1").version("2.1")
				.description("Contrato del back-end de las Iteraciones 1, 2 y 2.1 (CS-BO-01). Los errores salen como ProblemDetail, con la propiedad regla. "
						+ "x-permiso es el permiso MODULO:ACCION que exige cada operación (SESION: basta con iniciar sesión; PUBLICO: sin sesión)."));
			openApi.servers(List.of(new Server().url("/")));
			openApi.getComponents()
				.addSecuritySchemes(SESION, new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID"))
				.addSecuritySchemes(CSRF, new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-XSRF-TOKEN"));
			ModelConverters.getInstance().read(ProblemDetail.class).forEach(openApi.getComponents()::addSchemas);
			openApi.getPaths().forEach((ruta, oPathItem) -> oPathItem.readOperationsMap()
				.forEach((metodo, oOperation) -> completar(oOperation, ruta, metodo, datosDe(contrato, metodo + " " + ruta))));
		};
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> datosDe(Map<String, Object> contrato, String operacion) {
		Object datos = contrato.get(operacion);
		if (datos == null) {
			throw new IllegalStateException("openapi/contrato.yaml no describe " + operacion);
		}
		return (Map<String, Object>) datos;
	}

	@SuppressWarnings("unchecked")
	private static void completar(Operation oOperation, String ruta, PathItem.HttpMethod metodo, Map<String, Object> datos) {
		oOperation.setSummary((String) datos.get("resumen"));
		oOperation.addExtension("x-cus", datos.get("cus"));
		boolean publica = Boolean.TRUE.equals(datos.get("publica"));
		if (oOperation.getExtensions().get(X_PERMISO) == null) {
			oOperation.addExtension(X_PERMISO, publica ? PERMISO_PUBLICO : PERMISO_SESION);
		}
		completarSolicitud(oOperation, datos);
		Schema<?> esquemaExito = esquemaExito(oOperation);
		ApiResponses respuestas = new ApiResponses();
		Map<String, Object> propio403 = null;
		for (Map.Entry<String, Object> entrada : ((Map<String, Object>) datos.get("respuestas")).entrySet()) {
			if ("403".equals(entrada.getKey()) && !publica) {
				propio403 = (Map<String, Object>) entrada.getValue();
				continue;
			}
			respuestas.addApiResponse(entrada.getKey(), respuesta(entrada.getKey(), (Map<String, Object>) entrada.getValue(), esquemaExito));
		}
		if (!publica) {
			respuestas.addApiResponse("401", error("401", "Inicie sesión para continuar.", "SIN_SESION"));
			respuestas.addApiResponse("403", prohibido((String) oOperation.getExtensions().get(X_PERMISO), !SIN_CAMBIO_DE_CLAVE.contains(ruta), propio403));
			oOperation.setSecurity(metodo == PathItem.HttpMethod.GET ? List.of(new SecurityRequirement().addList(SESION))
					: List.of(new SecurityRequirement().addList(SESION).addList(CSRF)));
		}
		else {
			oOperation.setSecurity(List.of());
		}
		oOperation.setResponses(respuestas);
	}

	private static void completarSolicitud(Operation oOperation, Map<String, Object> datos) {
		if (oOperation.getRequestBody() == null) {
			return;
		}
		Content contenido = oOperation.getRequestBody().getContent();
		MediaType oMediaType = contenido.values().iterator().next();
		oMediaType.setExample(datos.get("solicitud"));
		String tipo = (String) datos.get("tipoSolicitud");
		if (tipo != null) {
			oOperation.getRequestBody().setContent(new Content().addMediaType(tipo, oMediaType));
		}
	}

	private static Schema<?> esquemaExito(Operation oOperation) {
		return oOperation.getResponses().values().stream()
			.filter(oRespuesta -> oRespuesta.getContent() != null)
			.flatMap(oRespuesta -> oRespuesta.getContent().values().stream())
			.map(oMediaType -> (Schema<?>) oMediaType.getSchema())
			.filter(oSchema -> oSchema != null && !String.valueOf(oSchema.get$ref()).endsWith(ESQUEMA_PROBLEMA))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Una respuesta 2xx: JSON con su ejemplo, o un archivo (tipo, o [It. 2] tipos: la exportación de reportes sale en PDF o Excel) sin ejemplo y
	 * con esquema binario.
	 */
	@SuppressWarnings("unchecked")
	private static ApiResponse respuesta(String estado, Map<String, Object> datos, Schema<?> esquemaExito) {
		String descripcion = (String) datos.get("descripcion");
		if (!estado.startsWith("2")) {
			return error(estado, descripcion, (String) datos.get("regla"));
		}
		ApiResponse oRespuesta = new ApiResponse().description(descripcion);
		Object tipos = datos.get("tipos");
		if (tipos instanceof List<?> || tipos instanceof Map<?, ?>) {
			List<?> lista = tipos instanceof List<?> enLista ? enLista : List.copyOf(((Map<String, ?>) tipos).values());
			Content contenido = new Content();
			lista.forEach(tipo -> contenido.addMediaType(String.valueOf(tipo), new MediaType().schema(new StringSchema().format("binary"))));
			return oRespuesta.content(contenido);
		}
		String tipo = (String) datos.getOrDefault("tipo", JSON);
		if (datos.containsKey("ejemplo") || datos.containsKey("tipo")) {
			oRespuesta.content(new Content().addMediaType(tipo, new MediaType().schema(esquemaExito).example(datos.get("ejemplo"))));
		}
		return oRespuesta;
	}

	/** [It. 2] El 403 con un ejemplo por causa: sin el permiso, clave temporal sin cambiar (RF82) y la regla propia de la operación, si tiene. */
	private static ApiResponse prohibido(String permiso, boolean conCambioDeClave, Map<String, Object> propio) {
		Map<String, Example> ejemplos = new LinkedHashMap<>();
		String sinPermiso = PERMISO_SESION.equals(permiso) ? "No tiene permiso para esta operación."
				: "No tiene el permiso " + permiso + " para esta operación.";
		ejemplos.put("sinPermiso", new Example().summary("Sin el permiso de la operación").value(problema(403, sinPermiso, "ACCESO_DENEGADO")));
		StringBuilder descripcion = new StringBuilder(sinPermiso + " (ACCESO_DENEGADO)");
		if (conCambioDeClave) {
			ejemplos.put("claveTemporal", new Example().summary("Con la clave temporal sin cambiar (CUS50, CUS54)")
				.value(problema(403, "Debe cambiar su clave temporal antes de continuar.", "RF82")));
			descripcion.append("; con la clave temporal sin cambiar (RF82)");
		}
		if (propio != null) {
			String regla = (String) propio.get("regla");
			ejemplos.put("regla", new Example().summary("Regla de la operación").value(problema(403, (String) propio.get("descripcion"), regla)));
			descripcion.append("; ").append(propio.get("descripcion")).append(" (").append(regla).append(")");
		}
		return new ApiResponse().description(descripcion.toString())
			.content(new Content().addMediaType(PROBLEMA,
					new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_PROBLEMA)).examples(ejemplos)));
	}

	private static ApiResponse error(String estado, String detalle, String regla) {
		int codigo = Integer.parseInt(estado);
		return new ApiResponse().description(detalle)
			.content(new Content().addMediaType(PROBLEMA,
					new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_PROBLEMA)).example(problema(codigo, detalle, regla))));
	}

	private static Map<String, Object> problema(int codigo, String detalle, String regla) {
		return Map.of("type", "about:blank", "title", HttpStatus.valueOf(codigo).getReasonPhrase(), "status", codigo, "detail", detalle, "regla",
				regla == null ? "" : regla);
	}

	private static Map<String, Object> leerContrato() {
		YamlMapFactoryBean lector = new YamlMapFactoryBean();
		lector.setResources(new ClassPathResource(RUTA_CONTRATO));
		return lector.getObject();
	}

}
