package dev.com.Synapsis_Spa_Cautiva.reportes.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.reportes.controller.ReporteDTO;
import dev.com.Synapsis_Spa_Cautiva.reportes.repository.FilaReporteDTO;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * [It. 2 · CUS49, RF74] Arma el archivo del reporte con sus columnas: EXCEL con Apache POI (poi-ooxml 6.0.0, B13) y PDF con OpenPDF (3.0.5, la
 * última estable de Maven Central, que ya generaba el PDF del comprobante). Primero el título, el rango y la sede; luego la tabla.
 */
@Component
public class GeneradorExportacion {

	private static final String CONSOLIDADO = "Todas las sedes (consolidado)";

	/** En 1/256 de carácter, la unidad de POI. */
	private static final int ANCHO_GRUPO = 45 * 256;

	private static final int ANCHO_VALOR = 20 * 256;

	public byte[] generar(ReporteDTO oReporteDTO, String formato) {
		return switch (FormatoExportacion.de(formato)) {
			case EXCEL -> excel(oReporteDTO);
			case PDF -> pdf(oReporteDTO);
		};
	}

	private static byte[] excel(ReporteDTO oReporteDTO) {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Sheet hoja = libro.createSheet(oReporteDTO.tipoReporte());
			XSSFFont negrita = libro.createFont();
			negrita.setBold(true);
			CellStyle encabezado = libro.createCellStyle();
			encabezado.setFont(negrita);
			hoja.createRow(0).createCell(0).setCellValue(oReporteDTO.titulo());
			hoja.createRow(1).createCell(0).setCellValue(subtitulo(oReporteDTO));
			List<ReporteDTO.Columna> columnas = oReporteDTO.columnas();
			Row titulos = hoja.createRow(3);
			for (int i = 0; i < columnas.size(); i++) {
				Cell celda = titulos.createCell(i);
				celda.setCellValue(columnas.get(i).titulo());
				celda.setCellStyle(encabezado);
			}
			int numero = 4;
			for (FilaReporteDTO oFila : oReporteDTO.filas()) {
				Row fila = hoja.createRow(numero++);
				for (int i = 0; i < columnas.size(); i++) {
					Object valor = valor(oFila, columnas.get(i).campo());
					Cell celda = fila.createCell(i);
					if (valor instanceof Number numeroValor) {
						celda.setCellValue(numeroValor.doubleValue());
					}
					else if (valor != null) {
						celda.setCellValue(valor.toString());
					}
				}
			}
			// Anchos fijos: autoSizeColumn necesita las fuentes de AWT, que un servidor sin entorno gráfico puede no tener
			hoja.setColumnWidth(0, ANCHO_GRUPO);
			for (int i = 1; i < columnas.size(); i++) {
				hoja.setColumnWidth(i, ANCHO_VALOR);
			}
			libro.write(salida);
			return salida.toByteArray();
		}
		catch (IOException ex) {
			throw new IllegalStateException("No se pudo armar el Excel del reporte", ex);
		}
	}

	private static byte[] pdf(ReporteDTO oReporteDTO) {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		Document documento = new Document(PageSize.A4.rotate());
		PdfWriter.getInstance(documento, salida);
		documento.open();
		documento.add(new Paragraph(oReporteDTO.titulo(), new Font(Font.HELVETICA, 14, Font.BOLD)));
		documento.add(new Paragraph(subtitulo(oReporteDTO), new Font(Font.HELVETICA, 9)));
		documento.add(new Paragraph(" "));
		List<ReporteDTO.Columna> columnas = oReporteDTO.columnas();
		PdfPTable tabla = new PdfPTable(columnas.size());
		tabla.setWidthPercentage(100);
		Font negrita = new Font(Font.HELVETICA, 9, Font.BOLD);
		Font normal = new Font(Font.HELVETICA, 9);
		columnas.forEach(columna -> tabla.addCell(new PdfPCell(new Phrase(columna.titulo(), negrita))));
		for (FilaReporteDTO oFila : oReporteDTO.filas()) {
			for (ReporteDTO.Columna columna : columnas) {
				Object valor = valor(oFila, columna.campo());
				PdfPCell celda = new PdfPCell(new Phrase(valor == null ? "" : texto(valor), normal));
				if (valor instanceof Number) {
					celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
				}
				tabla.addCell(celda);
			}
		}
		documento.add(tabla);
		documento.close();
		return salida.toByteArray();
	}

	private static String subtitulo(ReporteDTO oReporteDTO) {
		String sede = oReporteDTO.idSede() == null ? CONSOLIDADO : "Sede " + oReporteDTO.idSede();
		String agrupacion = oReporteDTO.agrupacion() == null ? "" : " · por " + oReporteDTO.agrupacion().toLowerCase().replace('_', ' ');
		return "Del " + oReporteDTO.fechaInicio() + " al " + oReporteDTO.fechaFin() + " · " + sede + agrupacion;
	}

	private static Object valor(FilaReporteDTO oFila, String campo) {
		return switch (campo) {
			case "grupo" -> oFila.grupo();
			case "cantidad" -> oFila.cantidad();
			case "importe" -> oFila.importe();
			case "costo" -> oFila.costo();
			case "margen" -> oFila.margen();
			case "stock" -> oFila.stock();
			case "minutos" -> oFila.minutos();
			default -> throw new IllegalArgumentException("Columna desconocida: " + campo);
		};
	}

	private static String texto(Object valor) {
		return valor instanceof BigDecimal monto ? monto.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : valor.toString();
	}

}
