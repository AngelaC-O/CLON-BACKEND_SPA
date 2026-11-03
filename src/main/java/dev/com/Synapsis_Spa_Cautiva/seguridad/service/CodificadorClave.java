package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CodificadorClave {

	private static final int COSTO = 10;

	private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(COSTO);

	public String codificar(String clave) {
		return codificador.encode(clave);
	}

}
