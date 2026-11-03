package dev.com.Synapsis_Spa_Cautiva;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** En producción no hay Swagger UI ni contrato publicado. No llama a ningún proveedor. */
@SpringBootTest(properties = { "APISPERU_CONSULTA_URL=http://localhost", "APISPERU_TOKEN=token", "APISPERU_FACTURACION_URL=http://localhost", "APISPERU_FACTURACION_TOKEN=token",
		"IZIPAY_API_URL=http://localhost", "IZIPAY_USUARIO=usuario", "IZIPAY_CLAVE=clave", "IZIPAY_HMAC_CLAVE=clave", "cautiva.comprobantes.reenvio-cron=-",
		"cautiva.archivos.directorio=${java.io.tmpdir}/cautiva-pruebas" })
@AutoConfigureMockMvc
@ActiveProfiles(value = "prod", inheritProfiles = false)
class PerfilProdTest extends OracleTestcontainer {

	@Autowired
	private MockMvc mvc;

	@Test
	void swaggerUiYElContratoEstanApagados() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
	}

}
