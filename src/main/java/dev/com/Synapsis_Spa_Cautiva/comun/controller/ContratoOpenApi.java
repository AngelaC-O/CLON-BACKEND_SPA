package dev.com.Synapsis_Spa_Cautiva.comun.controller;

import java.util.List;
import java.util.Map;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Completa lo que springdoc saca del código con openapi/contrato.yaml: resumen, CUS, estados y ejemplos de cada operación (U2 §5). De aquí sale
 * docs/openapi.yaml; ContratoOpenApiTest comprueba que los dos sigan iguales.
 */
@Configuration
class ContratoOpenApi {

	private static final String RUTA_CONTRATO = "openapi/contrato.yaml";

	private static final String JSON = "application/json";

	private static final String PROBLEMA = "application/problem+json";

	private static final String SESION = "sesion";

	private static final String CSRF = "csrf";

	private static final String ESQUEMA_PROBLEMA = "ProblemDetail";

	@Bean
	OpenApiCustomizer contratoDeLaIteracion() {
		Map<String, Object> contrato = leerContrato();
		return openApi -> {
			openApi.info(new Info().title("Cautiva Spa · BackOffice · Iteración 1").version("1.0")
				.description("Contrato del back-end de la Iteración 1 (CS-BO-01). Los errores salen como ProblemDetail, con la propiedad regla."));
			openApi.servers(List.of(new Server().url("/")));
			openApi.getComponents()
				.addSecuritySchemes(SESION, new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID"))
				.addSecuritySchemes(CSRF, new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-XSRF-TOKEN"));
			ModelConverters.getInstance().read(ProblemDetail.class).forEach(openApi.getComponents()::addSchemas);
			openApi.getPaths().forEach((ruta, oPathItem) -> oPathItem.readOperationsMap()
				.forEach((metodo, oOperation) -> completar(oOperation, metodo, datosDe(contrato, metodo + " " + ruta))));
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
	private static void completar(Operation oOperation, PathItem.HttpMethod metodo, Map<String, Object> datos) {
		oOperation.setSummary((String) datos.get("resumen"));
		oOperation.addExtension("x-cus", datos.get("cus"));
		completarSolicitud(oOperation, datos);
		Schema<?> esquemaExito = esquemaExito(oOperation);
		ApiResponses respuestas = new ApiResponses();
		((Map<String, Object>) datos.get("respuestas")).forEach((estado, valor) -> respuestas.addApiResponse(estado, respuesta(estado, (Map<String, Object>) valor, esquemaExito)));
		boolean publica = Boolean.TRUE.equals(datos.get("publica"));
		if (!publica) {
			respuestas.addApiResponse("401", error("401", "Inicie sesión para continuar.", "SIN_SESION"));
			respuestas.addApiResponse("403", error("403", "No tiene permiso para esta operación.", "ACCESO_DENEGADO"));
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

	private static ApiResponse respuesta(String estado, Map<String, Object> datos, Schema<?> esquemaExito) {
		String descripcion = (String) datos.get("descripcion");
		if (!estado.startsWith("2")) {
			return error(estado, descripcion, (String) datos.get("regla"));
		}
		ApiResponse oRespuesta = new ApiResponse().description(descripcion);
		String tipo = (String) datos.getOrDefault("tipo", JSON);
		if (datos.containsKey("ejemplo") || datos.containsKey("tipo")) {
			oRespuesta.content(new Content().addMediaType(tipo, new MediaType().schema(esquemaExito).example(datos.get("ejemplo"))));
		}
		return oRespuesta;
	}

	private static ApiResponse error(String estado, String detalle, String regla) {
		int codigo = Integer.parseInt(estado);
		Map<String, Object> ejemplo = Map.of("type", "about:blank", "title", HttpStatus.valueOf(codigo).getReasonPhrase(), "status", codigo, "detail", detalle, "regla",
				regla == null ? "" : regla);
		return new ApiResponse().description(detalle)
			.content(new Content().addMediaType(PROBLEMA,
					new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ESQUEMA_PROBLEMA)).example(ejemplo)));
	}

	private static Map<String, Object> leerContrato() {
		YamlMapFactoryBean lector = new YamlMapFactoryBean();
		lector.setResources(new ClassPathResource(RUTA_CONTRATO));
		return lector.getObject();
	}

}
