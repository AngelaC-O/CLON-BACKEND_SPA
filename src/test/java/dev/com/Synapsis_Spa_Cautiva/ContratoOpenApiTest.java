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

	/**
	 * Los 35 del draw.io de la Iteración 1, cambio de clave (DF-18), el comprobante público (DF-53), los 13 GET de los CUS modificados en la
	 * Iteración 2, los 10 de inventario (CUS66, CUS32 a CUS35) y los 15 de clientas, citas, espera, atención y catálogo (CUS03, CUS04, CUS06,
	 * CUS09, CUS10, CUS12, CUS58, CUS59 y CUS60), los 8 de venta de productos y anulación (CUS25, CUS26, CUS22, CUS23 y CUS24) y los 18 de
	 * caja, conciliación y configuración (CUS46,
	 * CUS47, CUS54, CUS55, CUS62, CUS63 y CUS65) y los 2 de reportes (CUS48 y CUS49).
	 */
	private static final int OPERACIONES = 103;

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

	/**
	 * [It. 2] Cada operación lleva x-permiso (Duda 1 del front-end): MODULO:ACCION, SESION o PUBLICO. El 403 de las que exigen sesión trae el
	 * ejemplo de la clave temporal sin cambiar (RF82), salvo las tres que FiltroCambioClave deja pasar.
	 */
	@Test
	@SuppressWarnings("unchecked")
	void cadaOperacionDiceSuPermisoYEl403IncluyeLaClaveTemporal() throws Exception {
		Map<String, Object> raiz = new Yaml().load(contrato());
		Map<String, Map<String, Object>> rutas = (Map<String, Map<String, Object>>) raiz.get("paths");
		List<String> problemas = new ArrayList<>();
		Set<String> sinCambioDeClave = Set.of("/api/v1/auth/cambio-clave", "/api/v1/auth/logout", "/api/v1/auth/sesion");
		for (var ruta : rutas.entrySet()) {
			for (var entrada : ruta.getValue().entrySet()) {
				if (!METODOS.contains(entrada.getKey())) {
					continue;
				}
				String nombre = entrada.getKey() + " " + ruta.getKey();
				Map<String, Object> operacion = (Map<String, Object>) entrada.getValue();
				String permiso = (String) operacion.get("x-permiso");
				if (permiso == null || !permiso.matches("[A-Z_]+:[A-Z_]+|SESION|PUBLICO")) {
					problemas.add(nombre + ": x-permiso no válido: " + permiso);
					continue;
				}
				Map<String, Map<String, Object>> respuestas = (Map<String, Map<String, Object>>) operacion.get("responses");
				if (!"PUBLICO".equals(permiso) && !sinCambioDeClave.contains(ruta.getKey())) {
					String prohibido = String.valueOf(respuestas.get("403"));
					if (!prohibido.contains("RF82") || !prohibido.contains("ACCESO_DENEGADO")) {
						problemas.add(nombre + ": el 403 no documenta ACCESO_DENEGADO y RF82");
					}
				}
			}
		}
		assertThat(problemas).isEmpty();
		assertThat(((Map<String, Object>) rutas.get("/api/v1/reportes/{tipoReporte}").get("get")).get("x-permiso")).isEqualTo("REPORTES:VER");
		assertThat(((Map<String, Object>) rutas.get("/api/v1/auth/login").get("post")).get("x-permiso")).isEqualTo("PUBLICO");
		assertThat(((Map<String, Object>) rutas.get("/api/v1/tipos-documento").get("get")).get("x-permiso")).isEqualTo("SESION");
	}

	/**
	 * Un PDF o un archivo binario no lleva ejemplo. En los demás, cada campo del ejemplo (o del primero de examples) existe en el esquema: un DTO
	 * renombrado no deja el ejemplo viejo.
	 */
	@SuppressWarnings("unchecked")
	private void revisarContenido(String nombre, Map<String, Map<String, Object>> contenido, Map<String, Map<String, Object>> esquemas, List<String> problemas) {
		contenido.forEach((tipo, medio) -> {
			Map<String, Object> esquemaMedio = (Map<String, Object>) medio.get("schema");
			if ("application/pdf".equals(tipo) || (esquemaMedio != null && "binary".equals(esquemaMedio.get("format")))) {
				return;
			}
			Object ejemplo = medio.get("example");
			if (ejemplo == null && medio.get("examples") instanceof Map<?, ?> ejemplos && !ejemplos.isEmpty()) {
				ejemplo = ((Map<String, Object>) ejemplos.values().iterator().next()).get("value");
			}
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
