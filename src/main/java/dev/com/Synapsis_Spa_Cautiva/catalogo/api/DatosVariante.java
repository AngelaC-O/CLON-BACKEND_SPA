package dev.com.Synapsis_Spa_Cautiva.catalogo.api;

/**
 * Lo que agenda necesita de una variante. tipoRecurso es texto porque el enum del dominio es interno. nombre es el del SERVICIO (así lo usan
 * la agenda y la lista de espera desde la Iteración 2). [It. 2.1 · nombres] nombreVariante es el de VARIANTE_SERVICIO; nombreServicio() da
 * «Servicio · Variante», como pide la página «02 Nombres en las respuestas».
 */
public record DatosVariante(Long idVariante, Long idServicio, Long idArea, Integer duracionRefMin, String tipoRecurso, String nombre,
		String nombreVariante) {

	private static final String SEPARADOR = " · ";

	/** «Corte de Cabello Dama · Normal»; sin nombre de variante, solo el del servicio. */
	public String nombreServicio() {
		return nombreVariante == null || nombreVariante.isBlank() ? nombre : nombre + SEPARADOR + nombreVariante;
	}

}
