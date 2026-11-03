package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;

import dev.com.Synapsis_Spa_Cautiva.pagos.service.AdaptadorIzipay;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.PasarelaPagosPort;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.AdaptadorApisPeruConsulta;
import dev.com.Synapsis_Spa_Cautiva.seguridad.service.ConsultaDocumentoPort;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.AdaptadorApisPeruFacturacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.FacturacionElectronicaPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/** application-sandbox.yaml arranca solo con sus variables y deja únicamente los adaptadores reales. No llama a ningún proveedor. */
@SpringBootTest(properties = { "APISPERU_TOKEN=token", "APISPERU_FACTURACION_TOKEN=token", "IZIPAY_USUARIO=usuario", "IZIPAY_CLAVE=clave", "IZIPAY_HMAC_CLAVE=clave",
		"cautiva.comprobantes.reenvio-cron=-", "cautiva.archivos.directorio=${java.io.tmpdir}/cautiva-pruebas" })
@ActiveProfiles(value = "sandbox", inheritProfiles = false)
class PerfilSandboxTest extends OracleTestcontainer {

	@Autowired
	private ApplicationContext contexto;

	@Test
	void usaSoloLosAdaptadoresReales() {
		assertThat(contexto.getBeansOfType(ConsultaDocumentoPort.class).values()).singleElement().isInstanceOf(AdaptadorApisPeruConsulta.class);
		assertThat(contexto.getBeansOfType(FacturacionElectronicaPort.class).values()).singleElement().isInstanceOf(AdaptadorApisPeruFacturacion.class);
		assertThat(contexto.getBeansOfType(PasarelaPagosPort.class).values()).singleElement().isInstanceOf(AdaptadorIzipay.class);
	}

}
