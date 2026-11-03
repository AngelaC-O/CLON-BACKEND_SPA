package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.util.Optional;

/** SPI: el usuario de la sesión. Lo implementa seguridad. */
public interface UsuarioActual {

	/** Vacío en el inicio de sesión y en los webhooks, que no tienen usuario. */
	Optional<Long> buscarIdUsuario();

	Long idUsuario();

	Long idSede();

}
