package dev.com.Synapsis_Spa_Cautiva.comun.api;

/** PKG_SESION en la conexión de la transacción actual. Solo el inicio de sesión lo llama a mano. */
public interface SesionOracle {

	void iniciar(Long idUsuario);

}
