package dev.com.Synapsis_Spa_Cautiva.reportes.service;

import java.util.Arrays;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

/** [It. 2 · CUS49, RF74] Los formatos de exportación, con su tipo de contenido y su extensión. */
public enum FormatoExportacion {

	EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),

	PDF("application/pdf", "pdf");

	private final String tipoContenido;

	private final String extension;

	FormatoExportacion(String tipoContenido, String extension) {
		this.tipoContenido = tipoContenido;
		this.extension = extension;
	}

	public static FormatoExportacion de(String formato) {
		return Arrays.stream(values())
			.filter(valor -> valor.name().equalsIgnoreCase(formato == null ? "" : formato.trim()))
			.findFirst()
			.orElseThrow(() -> new ReglaNegocioException("RF74", "Exporte en EXCEL o PDF."));
	}

	public String tipoContenido() {
		return tipoContenido;
	}

	public String extension() {
		return extension;
	}

}
