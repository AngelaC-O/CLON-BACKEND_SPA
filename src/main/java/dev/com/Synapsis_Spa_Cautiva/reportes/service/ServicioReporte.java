package dev.com.Synapsis_Spa_Cautiva.reportes.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.reportes.controller.ReporteDTO;
import dev.com.Synapsis_Spa_Cautiva.reportes.repository.FilaReporteDTO;
import dev.com.Synapsis_Spa_Cautiva.reportes.repository.RepositorioReporte;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [It. 2 · CUS48, CUS49, RF68 a RF74, RF93] Los reportes gerenciales: solo leen las vistas de reporte, sin entidades. Las fechas son obligatorias,
 * fechaInicio ≤ fechaFin y el rango es de hasta 366 días (422); idSede nulo es el consolidado (RF93); costos y márgenes solo para la
 * Administradora (RN24, 403); las ventas ANULADAS no cuentan (RN33, lo hacen las vistas).
 */
@Service
@RequiredArgsConstructor
public class ServicioReporte {

	/** Rango máximo de un reporte, con las dos fechas incluidas. */
	private static final long DIAS_MAXIMOS = 366;

	private final RepositorioReporte repositorioReporte;

	private final GeneradorExportacion generadorExportacion;

	private final UsuarioActual usuarioActual;

	@Transactional(readOnly = true)
	public ReporteDTO generar(String tipoReporte, LocalDate fechaInicio, LocalDate fechaFin, String agrupacion, Long idSede) {
		TipoReporte tipo = TipoReporte.deRuta(tipoReporte);
		validarRango(fechaInicio, fechaFin);
		if (tipo.conCostos() && !usuarioActual.esAdministradora(usuarioActual.idUsuario())) {
			throw new ReglaNegocioException("RN24", "Los costos y márgenes solo los ve la Administradora.", HttpStatus.FORBIDDEN);
		}
		String grupo = tipo.agrupacion(agrupacion);
		List<FilaReporteDTO> filas = switch (tipo) {
			case VENTAS -> repositorioReporte.ventasAgrupadas(fechaInicio, fechaFin, grupo, idSede);
			case PRODUCTOS -> repositorioReporte.productos(fechaInicio, fechaFin, idSede);
			case MARGEN -> repositorioReporte.margenServicios(fechaInicio, fechaFin, grupo, idSede);
			case DEMANDA -> repositorioReporte.demanda(fechaInicio, fechaFin, grupo, idSede);
			case OCUPACION -> repositorioReporte.ocupacion(fechaInicio, fechaFin, grupo, idSede);
			case ESPECIALISTAS -> repositorioReporte.ingresosEspecialista(fechaInicio, fechaFin, idSede);
		};
		return new ReporteDTO(tipo.ruta(), tipo.titulo(), grupo, fechaInicio, fechaFin, idSede, tipo.columnas(), filas);
	}

	/** CUS49 (RF74): el mismo reporte en EXCEL o PDF; formato inválido es 422. */
	@Transactional(readOnly = true)
	public byte[] exportar(String tipoReporte, LocalDate fechaInicio, LocalDate fechaFin, String agrupacion, Long idSede, String formato) {
		FormatoExportacion oFormato = FormatoExportacion.de(formato);
		ReporteDTO oReporteDTO = generar(tipoReporte, fechaInicio, fechaFin, agrupacion, idSede);
		return generadorExportacion.generar(oReporteDTO, oFormato.name());
	}

	private static void validarRango(LocalDate fechaInicio, LocalDate fechaFin) {
		if (fechaInicio == null || fechaFin == null) {
			throw new ReglaNegocioException("RF68", "Indique la fecha de inicio y la de fin del reporte.");
		}
		if (fechaInicio.isAfter(fechaFin)) {
			throw new ReglaNegocioException("RF68", "La fecha de inicio es posterior a la de fin.");
		}
		if (ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1 > DIAS_MAXIMOS) {
			throw new ReglaNegocioException("RF68", "El reporte abarca hasta " + DIAS_MAXIMOS + " días.");
		}
	}

}
