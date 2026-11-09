package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.yaml.snakeyaml.Yaml;
import org.springframework.test.web.servlet.MockMvc;

/**
 * docs/openapi.yaml es la salida de springdoc: si el código o openapi/contrato.yaml cambian, esta prueba falla. Para volver a exportarlo:
 * ./mvnw test -Dtest=ContratoOpenApiTest -Dcautiva.openapi.exportar=true
 */
@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
class ContratoOpenApiTest extends OracleTestcontainer {

	private static final Path ARCHIVO = Path.of("docs", "openapi.yaml");

	/** Los 35 del draw.io de la Iteración 1, cambio de clave (DF-18), el comprobante público (DF-53) y los 13 GET de los CUS modificados en la Iteración 2. */
	private static final int OPERACIONES = 50;

	private static final Set<String> METODOS = Set.of("get", "post", "put", "patch", "delete");

	@Autowired
	private MockMvc mvc;

	private String contrato() throws Exception {
		return mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	void docsOpenapiEsLaSalidaDelCodigo() throws Exception {
		String generado = contrato();
		if (Boolean.getBoolean("cautiva.openapi.exportar")) {
			Files.writeString(ARCHIVO, generado, StandardCharsets.UTF_8);
		}
		assertThat(Files.readString(ARCHIVO, StandardCharsets.UTF_8).replace("\r\n", "\n")).isEqualTo(generado.replace("\r\n", "\n"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void cadaOperacionTraeEjemplosQueCoincidenConSusEsquemas() throws Exception {
		Map<String, Object> raiz = new Yaml().load(contrato());
		Map<String, Map<String, Object>> rutas = (Map<String, Map<String, Object>>) raiz.get("paths");
		Map<String, Map<String, Object>> esquemas = (Map<String, Map<String, Object>>) ((Map<String, Object>) raiz.get("components")).get("schemas");
		List<String> problemas = new ArrayList<>();
		int operaciones = 0;
		for (var ruta : rutas.entrySet()) {
			for (var entrada : ruta.getValue().entrySet()) {
				if (!METODOS.contains(entrada.getKey())) {
					continue;
				}
				operaciones++;
				String nombre = entrada.getKey() + " " + ruta.getKey();
				Map<String, Object> operacion = (Map<String, Object>) entrada.getValue();
				if (operacion.get("x-cus") == null || operacion.get("summary") == null) {
					problemas.add(nombre + ": sin CUS o resumen");
				}
				Map<String, Object> cuerpo = (Map<String, Object>) operacion.get("requestBody");
				if (cuerpo != null) {
					revisarContenido(nombre + " solicitud", (Map<String, Map<String, Object>>) cuerpo.get("content"), esquemas, problemas);
				}
				Map<String, Map<String, Object>> respuestas = (Map<String, Map<String, Object>>) operacion.get("responses");
				respuestas.forEach((estado, respuesta) -> {
					if (respuesta.get("content") != null) {
						revisarContenido(nombre + " " + estado, (Map<String, Map<String, Object>>) respuesta.get("content"), esquemas, problemas);
					}
				});
			}
		}
		assertThat(problemas).isEmpty();
		assertThat(operaciones).isEqualTo(OPERACIONES);
	}

	/** Un PDF no lleva ejemplo. En los demás, cada campo del ejemplo existe en el esquema: un DTO renombrado no deja el ejemplo viejo. */
	@SuppressWarnings("unchecked")
	private void revisarContenido(String nombre, Map<String, Map<String, Object>> contenido, Map<String, Map<String, Object>> esquemas, List<String> problemas) {
		contenido.forEach((tipo, medio) -> {
			if ("application/pdf".equals(tipo)) {
				return;
			}
			Object ejemplo = medio.get("example");
			if (ejemplo == null) {
				problemas.add(nombre + ": sin ejemplo");
				return;
			}
			Object unEjemplo = ejemplo instanceof List<?> lista && !lista.isEmpty() ? lista.getFirst() : ejemplo;
			Map<String, Object> esquema = (Map<String, Object>) medio.get("schema");
			String ref = esquema == null ? null : (String) (esquema.get("$ref") != null ? esquema.get("$ref")
					: esquema.get("items") == null ? null : ((Map<String, Object>) esquema.get("items")).get("$ref"));
			if (ref != null && unEjemplo instanceof Map<?, ?> campos && !ref.endsWith("ProblemDetail")) {
				Map<String, Object> propiedades = (Map<String, Object>) esquemas.get(ref.substring(ref.lastIndexOf('/') + 1)).get("properties");
				campos.keySet().stream().filter(campo -> !propiedades.containsKey(campo)).forEach(campo -> problemas.add(nombre + ": el esquema no tiene " + campo));
			}
		});
	}

}
