package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.util.Optional;

/** SPI: el usuario de la sesión. Lo implementa seguridad. */
public interface UsuarioActual {

	/** Vacío en el inicio de sesión y en los webhooks, que no tienen usuario. */
	Optional<Long> buscarIdUsuario();

	Long idUsuario();

	Long idSede();

	/** [It. 2] La persona del personal de la sesión (CUS11: «Mi agenda» y RN50). */
	Long idPersonal();

	/** [It. 2] true si el usuario tiene el rol ADMINISTRADORA (RN50). */
	boolean esAdministradora(Long idUsuario);

}
