package dev.com.Synapsis_Spa_Cautiva.comun.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AlmacenArchivosPort;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Guarda en una carpeta del servidor. La ruta es relativa a esa carpeta y no puede salir de ella. */
@Component
class AlmacenArchivosLocal implements AlmacenArchivosPort {

	private final Path raiz;

	AlmacenArchivosLocal(@Value("${cautiva.archivos.directorio}") String directorio) {
		this.raiz = Path.of(directorio).toAbsolutePath().normalize();
	}

	@Override
	public String guardar(String rutaArchivo, String contenido) {
		Path destino = resolver(rutaArchivo);
		try {
			Files.createDirectories(destino.getParent());
			Files.writeString(destino, contenido, StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo guardar " + rutaArchivo, ex);
		}
		return rutaArchivo;
	}

	@Override
	public String leer(String rutaArchivo) {
		try {
			return Files.readString(resolver(rutaArchivo), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("No se pudo leer " + rutaArchivo, ex);
		}
	}

	private Path resolver(String rutaArchivo) {
		Path destino = raiz.resolve(rutaArchivo).normalize();
		if (!destino.startsWith(raiz)) {
			throw new IllegalArgumentException("Ruta fuera del almacén: " + rutaArchivo);
		}
		return destino;
	}

}
