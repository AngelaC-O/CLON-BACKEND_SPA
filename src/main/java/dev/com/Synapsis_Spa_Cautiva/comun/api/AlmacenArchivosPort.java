package dev.com.Synapsis_Spa_Cautiva.comun.api;

/** SPI: XML, CDR y PDF de los comprobantes. */
public interface AlmacenArchivosPort {

	/** Devuelve la ruta con la que se vuelve a leer. */
	String guardar(String rutaArchivo, String contenido);

	String leer(String rutaArchivo);

}
