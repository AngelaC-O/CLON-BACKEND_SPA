package dev.com.Synapsis_Spa_Cautiva.reportes.service;

import java.util.Arrays;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.reportes.controller.ReporteDTO;

import org.springframework.http.HttpStatus;

/**
 * [It. 2 · CUS48] Los tipos de reporte de la nota del diagrama, con su nombre en la ruta, la agrupación por defecto, si muestra costos y márgenes
 * (solo la Administradora, RN24) y sus columnas. Los nombres de la ruta los fija esta tarea (DF-133).
 */
public enum TipoReporte {

	/** RF68: por día, mes, línea, servicio, especialista, medio de pago o tipo de atención. */
	VENTAS("ventas", "Ventas", "DIA", false, columnas("grupo:Grupo", "cantidad:Ventas o unidades", "importe:Importe S/")),

	/** RF69: rotación, stock y margen de los productos. */
	PRODUCTOS("productos", "Productos: rotación, stock y margen", null, true,
			columnas("grupo:Producto", "cantidad:Unidades vendidas", "importe:Ingreso S/", "costo:Costo vendido S/", "margen:Margen sin IGV S/",
					"stock:Stock actual")),

	/** RF70: margen por servicio y por línea. */
	MARGEN("margen", "Margen por servicio y línea", "SERVICIO", true,
			columnas("grupo:Servicio o línea", "cantidad:Atenciones cobradas", "importe:Ingreso S/", "costo:Costo de insumos S/",
					"margen:Margen sin IGV S/")),

	/** RF71: demanda y días pico. */
	DEMANDA("demanda", "Demanda y días pico", "DIA_SEMANA", false, columnas("grupo:Grupo", "cantidad:Atenciones")),

	/** RF72: ocupación de recursos por franja. */
	OCUPACION("ocupacion", "Ocupación de recursos", "HORA", false, columnas("grupo:Franja o recurso", "cantidad:Bloques", "minutos:Minutos ocupados")),

	/** RF73: ingresos por especialista. */
	ESPECIALISTAS("especialistas", "Ingresos por especialista", null, false,
			columnas("grupo:Especialista", "cantidad:Servicios cobrados", "importe:Ingresos S/"));

	private final String ruta;

	private final String titulo;

	private final String agrupacionPorDefecto;

	private final boolean conCostos;

	private final List<ReporteDTO.Columna> columnas;

	TipoReporte(String ruta, String titulo, String agrupacionPorDefecto, boolean conCostos, List<ReporteDTO.Columna> columnas) {
		this.ruta = ruta;
		this.titulo = titulo;
		this.agrupacionPorDefecto = agrupacionPorDefecto;
		this.conCostos = conCostos;
		this.columnas = columnas;
	}

	/** El tipo de la ruta; uno desconocido es 404. */
	public static TipoReporte deRuta(String tipoReporte) {
		return Arrays.stream(values())
			.filter(tipo -> tipo.ruta.equalsIgnoreCase(tipoReporte == null ? "" : tipoReporte.trim()))
			.findFirst()
			.orElseThrow(() -> new ReglaNegocioException("RF68", "El reporte «" + tipoReporte + "» no existe: pida "
					+ String.join(", ", Arrays.stream(values()).map(TipoReporte::ruta).toList()) + ".", HttpStatus.NOT_FOUND));
	}

	public String ruta() {
		return ruta;
	}

	public String titulo() {
		return titulo;
	}

	/** Sin agrupación, la de por defecto; productos y especialistas no se agrupan de otra forma. */
	public String agrupacion(String pedida) {
		return pedida == null || pedida.isBlank() ? agrupacionPorDefecto : pedida.trim().toUpperCase();
	}

	public boolean conCostos() {
		return conCostos;
	}

	public List<ReporteDTO.Columna> columnas() {
		return columnas;
	}

	private static List<ReporteDTO.Columna> columnas(String... definiciones) {
		return Arrays.stream(definiciones).map(definicion -> {
			String[] partes = definicion.split(":", 2);
			return new ReporteDTO.Columna(partes[0], partes[1]);
		}).toList();
	}

}
