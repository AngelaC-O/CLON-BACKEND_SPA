package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Siempre responde ERROR: la ficha se ingresa a mano y queda MANUAL (RN45). Solo para desarrollo y pruebas (DF-50). */
@Component
@Profile({ "dev", "test" })
@ConditionalOnProperty(name = "cautiva.integraciones.apisperu.modo", havingValue = "falso")
class ConsultaDocumentoFalsa implements ConsultaDocumentoPort {

	private static final String ERROR = "ERROR";

	@Override
	public DatosPersona consultar(String codigoSunat, String numeroDocumento) {
		return new DatosPersona(ERROR, null, null, null);
	}

}
