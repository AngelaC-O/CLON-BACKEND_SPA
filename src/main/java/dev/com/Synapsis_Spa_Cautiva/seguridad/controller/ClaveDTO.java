package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS54, RF82] La clave temporal que escribe la Administradora. Solo se recibe: se guarda su BCrypt y nunca sale en una respuesta ni en
 * un log; la Administradora la ve una sola vez, en su pantalla.
 */
public record ClaveDTO(@NotBlank @Size(min = 8, max = 72) String clave) {

	/** Que la clave no aparezca si el objeto llega a un log. */
	@Override
	public String toString() {
		return "ClaveDTO[clave=****]";
	}

}
