package dev.com.Synapsis_Spa_Cautiva.caja.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.FilaLiquidacionDTO;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * [It. 2 · CUS47, RF105] Lee el reporte de liquidación que la Administradora descarga del panel de Izipay: CSV (separado por coma o punto y
 * coma) o Excel (.xlsx o .xls, la primera hoja). Izipay no publica una API de liquidación ni el formato del reporte (DF-49), así que los nombres
 * de las tres columnas que importan se configuran en application.yaml (cautiva.liquidacion.*) y se comparan sin mayúsculas, tildes ni espacios
 * de más. Las demás columnas se ignoran; una fila sin código de operación se salta.
 */
@Component
public class LectorReporteLiquidacion {

	private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ISO_LOCAL_DATE,
			DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy"));

	private final String columnaCodigo;

	private final String columnaComision;

	private final String columnaFechaAbono;

	public LectorReporteLiquidacion(@Value("${cautiva.liquidacion.columna-codigo-operacion}") String columnaCodigo,
			@Value("${cautiva.liquidacion.columna-comision}") String columnaComision,
			@Value("${cautiva.liquidacion.columna-fecha-abono}") String columnaFechaAbono) {
		this.columnaCodigo = normalizar(columnaCodigo);
		this.columnaComision = normalizar(columnaComision);
		this.columnaFechaAbono = normalizar(columnaFechaAbono);
	}

	public List<FilaLiquidacionDTO> leerReporte(MultipartFile archivoLiquidacion) {
		if (archivoLiquidacion == null || archivoLiquidacion.isEmpty()) {
			throw new ReglaNegocioException("RF105", "Adjunte el reporte de liquidación descargado de Izipay.");
		}
		String nombre = archivoLiquidacion.getOriginalFilename() == null ? "" : archivoLiquidacion.getOriginalFilename().toLowerCase(Locale.ROOT);
		try (InputStream contenido = archivoLiquidacion.getInputStream()) {
			List<List<String>> tabla = nombre.endsWith(".xlsx") || nombre.endsWith(".xls") ? leerExcel(contenido) : leerCsv(contenido);
			return aFilas(tabla);
		}
		catch (IOException ex) {
			throw new ReglaNegocioException("RF105", "No se pudo leer el reporte de liquidación: " + ex.getMessage());
		}
	}

	private List<FilaLiquidacionDTO> aFilas(List<List<String>> tabla) {
		if (tabla.isEmpty()) {
			throw new ReglaNegocioException("RF105", "El reporte de liquidación está vacío.");
		}
		List<String> encabezado = tabla.getFirst().stream().map(LectorReporteLiquidacion::normalizar).toList();
		int iCodigo = columna(encabezado, columnaCodigo);
		int iComision = columna(encabezado, columnaComision);
		int iFecha = columna(encabezado, columnaFechaAbono);
		List<FilaLiquidacionDTO> filas = new ArrayList<>();
		for (int i = 1; i < tabla.size(); i++) {
			List<String> fila = tabla.get(i);
			String codigo = celda(fila, iCodigo);
			if (codigo == null) {
				continue;
			}
			filas.add(new FilaLiquidacionDTO(codigo, monto(celda(fila, iComision), i + 1), fecha(celda(fila, iFecha), i + 1)));
		}
		return filas;
	}

	private static int columna(List<String> encabezado, String buscada) {
		int indice = encabezado.indexOf(buscada);
		if (indice < 0) {
			throw new ReglaNegocioException("RF105", "El reporte no tiene la columna «" + buscada + "» (ver cautiva.liquidacion en la configuración).");
		}
		return indice;
	}

	private static String celda(List<String> fila, int indice) {
		if (indice >= fila.size()) {
			return null;
		}
		String valor = fila.get(indice).trim();
		return valor.isEmpty() ? null : valor;
	}

	/** «S/ 1,05», «1.05» o «1,05»: sin moneda y con la coma decimal como punto. */
	private static BigDecimal monto(String texto, int numeroFila) {
		if (texto == null) {
			return null;
		}
		String limpio = texto.replace("S/", "").replace("PEN", "").replace(" ", "");
		if (limpio.contains(",") && !limpio.contains(".")) {
			limpio = limpio.replace(',', '.');
		}
		else {
			limpio = limpio.replace(",", "");
		}
		try {
			return new BigDecimal(limpio);
		}
		catch (NumberFormatException ex) {
			throw new ReglaNegocioException("RF105", "La comisión de la fila " + numeroFila + " no es un número: " + texto);
		}
	}

	private static LocalDate fecha(String texto, int numeroFila) {
		if (texto == null) {
			return null;
		}
		String soloFecha = texto.length() > 10 ? texto.substring(0, 10) : texto;
		for (DateTimeFormatter formato : FORMATOS_FECHA) {
			try {
				return LocalDate.parse(soloFecha, formato);
			}
			catch (DateTimeParseException ex) {
				// prueba el siguiente formato
			}
		}
		throw new ReglaNegocioException("RF105", "La fecha de abono de la fila " + numeroFila + " no se entiende: " + texto);
	}

	private static List<List<String>> leerCsv(InputStream contenido) throws IOException {
		BufferedReader lector = new BufferedReader(new InputStreamReader(contenido, StandardCharsets.UTF_8));
		List<List<String>> tabla = new ArrayList<>();
		String linea;
		char separador = 0;
		while ((linea = lector.readLine()) != null) {
			if (tabla.isEmpty()) {
				linea = linea.replace("﻿", "");
				separador = contarFuera(linea, ';') > contarFuera(linea, ',') ? ';' : ',';
			}
			if (!linea.isBlank()) {
				tabla.add(partir(linea, separador));
			}
		}
		return tabla;
	}

	/** Un CSV simple: comillas dobles para los campos con separador y "" para una comilla dentro. */
	private static List<String> partir(String linea, char separador) {
		List<String> campos = new ArrayList<>();
		StringBuilder campo = new StringBuilder();
		boolean entreComillas = false;
		for (int i = 0; i < linea.length(); i++) {
			char c = linea.charAt(i);
			if (c == '"') {
				if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
					campo.append('"');
					i++;
				}
				else {
					entreComillas = !entreComillas;
				}
			}
			else if (c == separador && !entreComillas) {
				campos.add(campo.toString());
				campo.setLength(0);
			}
			else {
				campo.append(c);
			}
		}
		campos.add(campo.toString());
		return campos;
	}

	private static int contarFuera(String linea, char buscado) {
		int cuenta = 0;
		boolean entreComillas = false;
		for (char c : linea.toCharArray()) {
			if (c == '"') {
				entreComillas = !entreComillas;
			}
			else if (c == buscado && !entreComillas) {
				cuenta++;
			}
		}
		return cuenta;
	}

	/** La primera hoja; las fechas de Excel salen como yyyy-MM-dd y los números sin formato regional. */
	private static List<List<String>> leerExcel(InputStream contenido) throws IOException {
		DataFormatter formato = new DataFormatter(Locale.ROOT);
		List<List<String>> tabla = new ArrayList<>();
		try (Workbook libro = WorkbookFactory.create(contenido)) {
			Sheet hoja = libro.getSheetAt(0);
			for (Row fila : hoja) {
				List<String> valores = new ArrayList<>();
				for (int i = 0; i < fila.getLastCellNum(); i++) {
					valores.add(textoDe(fila.getCell(i), formato));
				}
				if (valores.stream().anyMatch(valor -> !valor.isBlank())) {
					tabla.add(valores);
				}
			}
		}
		return tabla;
	}

	private static String textoDe(Cell celda, DataFormatter formato) {
		if (celda == null) {
			return "";
		}
		if (celda.getCellType() == CellType.NUMERIC) {
			return DateUtil.isCellDateFormatted(celda) ? celda.getLocalDateTimeCellValue().toLocalDate().toString()
					: BigDecimal.valueOf(celda.getNumericCellValue()).stripTrailingZeros().toPlainString();
		}
		return formato.formatCellValue(celda);
	}

	private static String normalizar(String texto) {
		String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

}
