package dev.com.Synapsis_Spa_Cautiva.comun.api;

import lombok.Getter;

import org.springframework.http.HttpStatus;

/** Regla de negocio incumplida antes de llegar a Oracle (ADR-05). Por defecto responde 422. */
@Getter
public class ReglaNegocioException extends RuntimeException {

	private final String regla;

	private final HttpStatus estado;

	public ReglaNegocioException(String regla, String mensaje) {
		this(regla, mensaje, HttpStatus.UNPROCESSABLE_CONTENT);
	}

	public ReglaNegocioException(String regla, String mensaje, HttpStatus estado) {
		super(mensaje);
		this.regla = regla;
		this.estado = estado;
	}

}
