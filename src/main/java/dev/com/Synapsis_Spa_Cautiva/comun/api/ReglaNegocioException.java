package dev.com.Synapsis_Spa_Cautiva.comun.api;

import lombok.Getter;

/** Regla de negocio incumplida en el dominio, antes de llegar a Oracle (ADR-05). */
@Getter
public class ReglaNegocioException extends RuntimeException {

	private final String regla;

	public ReglaNegocioException(String regla, String mensaje) {
		super(regla + ": " + mensaje);
		this.regla = regla;
	}

}
