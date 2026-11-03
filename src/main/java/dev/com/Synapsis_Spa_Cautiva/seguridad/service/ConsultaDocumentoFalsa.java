package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import org.springframework.stereotype.Component;

/** Siempre responde ERROR: la ficha se ingresa a mano y queda MANUAL (RN45). */
@Component
class ConsultaDocumentoFalsa implements ConsultaDocumentoPort {

	private static final String ERROR = "ERROR";

	@Override
	public DatosPersona consultar(String codigoSunat, String numeroDocumento) {
		return new DatosPersona(ERROR, null, null, null);
	}

}
