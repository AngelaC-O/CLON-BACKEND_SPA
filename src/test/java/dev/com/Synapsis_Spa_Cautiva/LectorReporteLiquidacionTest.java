package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.caja.service.LectorReporteLiquidacion;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.FilaLiquidacionDTO;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * [It. 2 · CUS47, RF105] El lector del reporte de liquidación de Izipay, sin Oracle ni Spring: el CSV de ejemplo de src/test/resources, un Excel
 * armado aquí y un reporte al que le falta una columna. Los nombres de columna son los de application.yaml.
 */
class LectorReporteLiquidacionTest {

	private final LectorReporteLiquidacion lector = new LectorReporteLiquidacion("Número de pedido", "Comisión", "Fecha de abono");

	@Test
	void leeElCsvDeEjemploConPuntoYComaYComaDecimal() throws Exception {
		byte[] contenido;
		try (InputStream ejemplo = getClass().getResourceAsStream("/liquidacion/reporte-liquidacion-ejemplo.csv")) {
			contenido = ejemplo.readAllBytes();
		}

		List<FilaLiquidacionDTO> filas = lector.leerReporte(new MockMultipartFile("archivoLiquidacion", "reporte.csv", "text/csv", contenido));

		assertThat(filas).containsExactly(new FilaLiquidacionDTO("CS-EJEMPLO-0001", new BigDecimal("1.75"), LocalDate.of(2026, 11, 7)),
				new FilaLiquidacionDTO("CS-EJEMPLO-9999", new BigDecimal("0.70"), LocalDate.of(2026, 11, 7)));
	}

	@Test
	void leeUnExcelConLaFechaComoFechaYLosEncabezadosSinTildes() throws Exception {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		try (XSSFWorkbook libro = new XSSFWorkbook()) {
			Sheet hoja = libro.createSheet("Liquidación");
			Row encabezado = hoja.createRow(0);
			encabezado.createCell(0).setCellValue("NUMERO DE PEDIDO");
			encabezado.createCell(1).setCellValue("comision");
			encabezado.createCell(2).setCellValue("  Fecha   de abono ");
			Row fila = hoja.createRow(1);
			fila.createCell(0).setCellValue("CS-EXCEL-1");
			fila.createCell(1).setCellValue(2.5);
			CellStyle fecha = libro.createCellStyle();
			fecha.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
			fila.createCell(2).setCellValue(LocalDateTime.of(2026, 11, 8, 0, 0));
			fila.getCell(2).setCellStyle(fecha);
			libro.write(salida);
		}

		List<FilaLiquidacionDTO> filas = lector.leerReporte(new MockMultipartFile("archivoLiquidacion", "reporte.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", salida.toByteArray()));

		assertThat(filas).containsExactly(new FilaLiquidacionDTO("CS-EXCEL-1", new BigDecimal("2.5"), LocalDate.of(2026, 11, 8)));
	}

	@Test
	void unReporteSinLaColumnaConfiguradaOVacioEs422() {
		byte[] sinComision = "Número de pedido,Fecha de abono\nCS-1,07/11/2026\n".getBytes(StandardCharsets.UTF_8);
		assertThatThrownBy(() -> lector.leerReporte(new MockMultipartFile("archivoLiquidacion", "reporte.csv", "text/csv", sinComision)))
			.isInstanceOf(ReglaNegocioException.class)
			.hasMessageContaining("comision");
		assertThatThrownBy(() -> lector.leerReporte(new MockMultipartFile("archivoLiquidacion", "vacio.csv", "text/csv", new byte[0])))
			.isInstanceOf(ReglaNegocioException.class);
	}

}
