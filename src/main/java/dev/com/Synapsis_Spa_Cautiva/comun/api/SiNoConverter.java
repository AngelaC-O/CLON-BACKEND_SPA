package dev.com.Synapsis_Spa_Cautiva.comun.api;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Columnas CHAR(1) 'S'/'N' del diccionario v5 como Boolean. */
@Converter
public class SiNoConverter implements AttributeConverter<Boolean, String> {

	@Override
	public String convertToDatabaseColumn(Boolean valor) {
		if (valor == null) {
			return null;
		}
		return valor ? "S" : "N";
	}

	@Override
	public Boolean convertToEntityAttribute(String columna) {
		if (columna == null) {
			return null;
		}
		return "S".equals(columna);
	}

}
