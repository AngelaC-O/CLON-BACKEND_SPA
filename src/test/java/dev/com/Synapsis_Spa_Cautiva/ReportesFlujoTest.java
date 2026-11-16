package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import dev.com.Synapsis_Spa_Cautiva.seguridad.service.UsuarioSesion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.FacturacionElectronicaPort;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.RespuestaCpe;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * [It. 2] CUS48 y CUS49 con datos propios en la sede de la clase (VentasFlujoApoyo), preparados una vez: una venta de un Corte en efectivo, otra
 * igual ANULADA (SUNAT rechazó su boleta y la Administradora aprobó la solicitud sin nota de crédito), que ningún reporte debe sumar (RN33), y
 * una venta de 2 unidades de un producto propio. Los reportes se piden con idSede de la clase, así que las demás clases no cambian las cifras.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportesFlujoTest extends VentasFlujoApoyo {

	private static final String TOKEN_CSRF = "token-de-prueba";

	/** Cubre las atenciones que VentasFlujoApoyo inserta en días pasados y las ventas de hoy. */
	private static final LocalDate DESDE = LocalDate.now().minusDays(300);

	private static final LocalDate HASTA = LocalDate.now();

	private static boolean preparado;

	private static Long idProducto;

	private static BigDecimal precioProducto;

	@MockitoSpyBean
	private FacturacionElectronicaPort facturacionElectronicaPort;

	@BeforeEach
	void preparar() throws Exception {
		if (preparado) {
			return;
		}
		cajaAbierta();
		ventaCerrada();

		// La venta ANULADA: SUNAT rechaza su boleta y la Administradora anula sin nota de crédito
		doReturn(new RespuestaCpe(EstadoSunat.RECHAZADO, "2017", "Rechazado por SUNAT", null, null, null)).when(facturacionElectronicaPort).emitir(any());
		Long anulada = idDe(ventaCerrada(), "idVenta");
		reset(facturacionElectronicaPort);
		Long idSolicitud = idDe(enviar(post("/api/v1/ventas/" + anulada + "/solicitudes-anulacion"),
				"{\"motivo\":\"Prueba de reportes\",\"codigoMotivo\":\"01\",\"formaDevolucion\":\"MEDIO_ORIGINAL\"}").andExpect(status().isCreated()), "idSolicitud");
		como("admin", post("/api/v1/solicitudes-anulacion/" + idSolicitud + "/aprobacion"), null).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("SELECT estado FROM VENTA WHERE id_venta = ?", String.class, anulada)).isEqualTo("ANULADA");

		// Dos unidades de un producto propio (CUS32, CUS34 y CUS26)
		Long idCategoria = id("SELECT MIN(id_categoria) FROM CATEGORIA_PRODUCTO WHERE nombre <> 'Insumos de servicio'");
		idProducto = idDe(como("almacen", post("/api/v1/productos"), "{\"codigo\":\"REP-" + sedeDeLaClase() + "\",\"nombre\":\"Crema de reportes "
				+ sedeDeLaClase() + "\",\"idCategoria\":" + idCategoria + ",\"unidad\":\"UND\",\"tipoUso\":\"VENTA\",\"costo\":10.00,\"stockMinimo\":0}")
			.andExpect(status().isCreated()), "idProducto");
		como("almacen", post("/api/v1/productos/" + idProducto + "/ajustes"), "{\"cantidad\":5,\"motivo\":\"Conteo inicial\"}").andExpect(status().isOk());
		precioProducto = jdbc.queryForObject("SELECT precio_venta FROM PRODUCTO WHERE id_producto = ?", BigDecimal.class, idProducto);
		enviar(post("/api/v1/ventas/productos"), "{\"lineas\":[{\"idProducto\":" + idProducto + ",\"cantidad\":2}],\"idMedioPago\":" + medio("EFECTIVO")
				+ ",\"monto\":" + precioProducto.multiply(BigDecimal.TWO).toPlainString() + ",\"tipo\":\"BOLETA\"}").andExpect(status().isCreated());
		preparado = true;
	}

	// ------------------------------------------------------------------ apoyo

	private ResultActions como(String usuario, MockHttpServletRequestBuilder pedido, String cuerpo) throws Exception {
		return enviarEnSede(usuario, sedeDeLaClase(), pedido, cuerpo);
	}

	/** Un reporte de la sede de la clase entre DESDE y HASTA, pedido por la Administradora. */
	private ResultActions reporte(String tipo, String agrupacion) throws Exception {
		MockHttpServletRequestBuilder pedido = get("/api/v1/reportes/" + tipo).param("fechaInicio", DESDE.toString()).param("fechaFin", HASTA.toString())
			.param("idSede", sedeDeLaClase().toString());
		if (agrupacion != null) {
			pedido.param("agrupacion", agrupacion);
		}
		return como("admin", pedido, null);
	}

	/** RN24: una usuaria con REPORTES:VER y REPORTES:EXPORTAR que no es ADMINISTRADORA (ningún rol del script lo tiene: se arma solo aquí). */
	private ResultActions comoNoAdministradora(MockHttpServletRequestBuilder pedido) throws Exception {
		Long idUsuario = id("SELECT id_usuario FROM USUARIO WHERE nombre_usuario = 'recepcion'");
		Set<String> permisos = Set.of("REPORTES:VER", "REPORTES:EXPORTAR");
		UsuarioSesion principal = new UsuarioSesion(idUsuario, "recepcion", sedeDeLaClase(), false, permisos);
		pedido.with(authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null,
				permisos.stream().map(SimpleGrantedAuthority::new).toList())))
			.cookie(new Cookie("XSRF-TOKEN", TOKEN_CSRF))
			.header("X-XSRF-TOKEN", TOKEN_CSRF);
		return mvc.perform(pedido);
	}

	private String hoy() {
		return HASTA.toString();
	}

	private BigDecimal ventaProducto() {
		return precioProducto.multiply(BigDecimal.TWO);
	}

	// ------------------------------------------------------------------ CUS48: ventas (RF68) y RN33

	@Test
	void ventasPorDiaLineaMedioYEspecialistaSinLaAnulada() throws Exception {
		// Hoy: el Corte y la venta del producto; la anulada no cuenta (RN33)
		reporte("ventas", "DIA").andExpect(status().isOk())
			.andExpect(jsonPath("$.tipoReporte").value("ventas"))
			.andExpect(jsonPath("$.columnas[2].campo").value("importe"))
			.andExpect(jsonPath("$.filas", hasSize(1)))
			.andExpect(jsonPath("$.filas[0].grupo").value(hoy()))
			.andExpect(jsonPath("$.filas[0].cantidad").value(2))
			.andExpect(jsonPath("$.filas[0].importe").value(PRECIO_CORTE.add(ventaProducto()).doubleValue()))
			.andExpect(jsonPath("$.filas[0].costo").doesNotExist());
		reporte("ventas", "LINEA").andExpect(status().isOk())
			.andExpect(jsonPath("$.filas[?(@.grupo == 'SALON')].cantidad", hasItem(1)))
			.andExpect(jsonPath("$.filas[?(@.grupo == 'Productos')].cantidad", hasItem(2)));
		reporte("ventas", "MEDIO_PAGO").andExpect(status().isOk())
			.andExpect(jsonPath("$.filas[?(@.grupo == 'EFECTIVO')].cantidad", hasItem(2)));
		reporte("ventas", "TIPO_ATENCION").andExpect(status().isOk())
			.andExpect(jsonPath("$.filas[?(@.grupo == 'SIN_CITA')].cantidad", hasItem(1)));
		reporte("ventas", "MES").andExpect(status().isOk()).andExpect(jsonPath("$.filas[0].cantidad").value(2));

		// Sin idSede, el consolidado de todas las sedes (RF93); una agrupación que no es de ventas es 422
		como("admin", get("/api/v1/reportes/ventas").param("fechaInicio", hoy()).param("fechaFin", hoy()), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.idSede").doesNotExist());
		reporte("ventas", "RECURSO").andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RF68"));
	}

	// ------------------------------------------------------------------ productos (RF69) y margen (RF70), RN24

	@Test
	void productosConRotacionStockYMargenSoloParaLaAdministradora() throws Exception {
		reporte("productos", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.columnas[*].campo", hasItem("margen")))
			.andExpect(jsonPath("$.filas[?(@.grupo == 'REP-" + sedeDeLaClase() + " - Crema de reportes " + sedeDeLaClase() + "')].cantidad", hasItem(2)))
			.andExpect(jsonPath("$.filas[?(@.grupo == 'REP-" + sedeDeLaClase() + " - Crema de reportes " + sedeDeLaClase() + "')].costo", hasItem(20)))
			.andExpect(jsonPath("$.filas[?(@.grupo == 'REP-" + sedeDeLaClase() + " - Crema de reportes " + sedeDeLaClase() + "')].stock", hasItem(3)));

		comoNoAdministradora(get("/api/v1/reportes/productos").param("fechaInicio", hoy()).param("fechaFin", hoy())).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.regla").value("RN24"));
		comoNoAdministradora(get("/api/v1/reportes/margen").param("fechaInicio", hoy()).param("fechaFin", hoy())).andExpect(status().isForbidden());
		comoNoAdministradora(get("/api/v1/reportes/ventas").param("fechaInicio", hoy()).param("fechaFin", hoy())).andExpect(status().isOk());
	}

	@Test
	void d37DosProductosConElMismoNombreNoSeMezclan() throws Exception {
		// Otro producto con el mismo nombre y otro código, sin ventas: antes se unía por el nombre y tomaba las 2 unidades del primero
		String nombre = "Crema de reportes " + sedeDeLaClase();
		String codigo = "REP2-" + sedeDeLaClase();
		if (id("SELECT COUNT(*) FROM PRODUCTO WHERE codigo = ?", codigo) == 0) {
			Long idCategoria = id("SELECT id_categoria FROM PRODUCTO WHERE id_producto = ?", idProducto);
			como("almacen", post("/api/v1/productos"), "{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre + "\",\"idCategoria\":" + idCategoria
					+ ",\"unidad\":\"UND\",\"tipoUso\":\"VENTA\",\"costo\":10.00,\"stockMinimo\":0}").andExpect(status().isCreated());
		}

		// [It. 2.1 · D-37] V_VENTA_LINEA se une por id_producto: cada producto con lo suyo
		reporte("productos", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.filas[?(@.grupo == 'REP-" + sedeDeLaClase() + " - " + nombre + "')].cantidad", hasItem(2)))
			.andExpect(jsonPath("$.filas[?(@.grupo == '" + codigo + " - " + nombre + "')].cantidad", hasItem(0)))
			.andExpect(jsonPath("$.filas[?(@.grupo == '" + codigo + " - " + nombre + "')].importe", hasItem(0)));
	}

	@Test
	void margenPorServicioYLineaSinLaAnulada() throws Exception {
		reporte("margen", "SERVICIO").andExpect(status().isOk())
			.andExpect(jsonPath("$.agrupacion").value("SERVICIO"))
			.andExpect(jsonPath("$.filas[?(@.grupo =~ /" + CORTE + " - .*/)].cantidad", hasItem(1)))
			.andExpect(jsonPath("$.filas[?(@.grupo =~ /" + CORTE + " - .*/)].importe", hasItem(PRECIO_CORTE.intValue())));
		reporte("margen", "LINEA").andExpect(status().isOk()).andExpect(jsonPath("$.filas[?(@.grupo == 'SALON')].cantidad", hasItem(1)));
	}

	// ------------------------------------------------------------------ demanda (RF71), ocupación (RF72) y especialistas (RF73)

	@Test
	void demandaOcupacionEIngresosPorEspecialista() throws Exception {
		// Las dos atenciones del Corte (también la de la venta anulada: la demanda cuenta atenciones, no ventas)
		List<Integer> porDia = com.jayway.jsonpath.JsonPath.read(reporte("demanda", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.agrupacion").value("DIA_SEMANA"))
			.andReturn().getResponse().getContentAsString(), "$.filas[*].cantidad");
		assertThat(porDia.stream().mapToInt(Integer::intValue).sum()).isEqualTo(2);
		reporte("demanda", "SERVICIO").andExpect(status().isOk()).andExpect(jsonPath("$.filas[?(@.grupo =~ /" + CORTE + " - .*/)].cantidad", hasItem(2)));

		String sillon = jdbc.queryForObject("SELECT nombre FROM RECURSO WHERE id_recurso = ?", String.class, recurso(0));
		reporte("ocupacion", "RECURSO").andExpect(status().isOk())
			.andExpect(jsonPath("$.filas[?(@.grupo == '" + sillon + " (SILLON)')].cantidad", hasItem(2)));
		reporte("ocupacion", null).andExpect(status().isOk()).andExpect(jsonPath("$.agrupacion").value("HORA"))
			.andExpect(jsonPath("$.filas[0].minutos").isNumber());

		// Solo el Corte cobrado: la venta anulada no suma (RN33)
		reporte("especialistas", null).andExpect(status().isOk())
			.andExpect(jsonPath("$.filas", hasSize(1)))
			.andExpect(jsonPath("$.filas[0].grupo").value("Especialista De Ventas"))
			.andExpect(jsonPath("$.filas[0].cantidad").value(1))
			.andExpect(jsonPath("$.filas[0].importe").value(PRECIO_CORTE.doubleValue()));
	}

	// ------------------------------------------------------------------ reglas de las fechas y permisos

	@Test
	void unRangoInvalidoEs422UnTipoInexistente404YLaRecepcionista403() throws Exception {
		como("admin", get("/api/v1/reportes/ventas").param("fechaInicio", hoy()).param("fechaFin", HASTA.minusDays(1).toString()), null)
			.andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RF68"));
		como("admin", get("/api/v1/reportes/ventas").param("fechaInicio", HASTA.minusDays(366).toString()).param("fechaFin", hoy()), null)
			.andExpect(status().isUnprocessableContent());
		como("admin", get("/api/v1/reportes/ventas").param("fechaInicio", HASTA.minusDays(365).toString()).param("fechaFin", hoy()), null)
			.andExpect(status().isOk());
		como("admin", get("/api/v1/reportes/ventas"), null).andExpect(status().isUnprocessableContent());
		como("admin", get("/api/v1/reportes/inventado").param("fechaInicio", hoy()).param("fechaFin", hoy()), null).andExpect(status().isNotFound());

		enviar(get("/api/v1/reportes/ventas").param("fechaInicio", hoy()).param("fechaFin", hoy()), null).andExpect(status().isForbidden());
		enviar(get("/api/v1/reportes/ventas/exportacion").param("fechaInicio", hoy()).param("fechaFin", hoy()).param("formato", "PDF"), null)
			.andExpect(status().isForbidden());
	}

	// ------------------------------------------------------------------ CUS49

	@Test
	void exportaEnExcelYEnPdfConSuTipoYSuNombre() throws Exception {
		String desde = DESDE.toString();
		byte[] excel = como("admin", get("/api/v1/reportes/ventas/exportacion").param("fechaInicio", desde).param("fechaFin", hoy())
			.param("agrupacion", "LINEA").param("idSede", sedeDeLaClase().toString()).param("formato", "EXCEL"), null)
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
			.andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"reporte_ventas_" + desde + "_" + hoy() + ".xlsx\"")))
			.andReturn().getResponse().getContentAsByteArray();
		assertThat(new String(excel, 0, 2)).isEqualTo("PK");
		try (var libro = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(excel))) {
			assertThat(libro.getSheetAt(0).getRow(3).getCell(0).getStringCellValue()).isEqualTo("Grupo");
			assertThat(libro.getSheetAt(0).getRow(4).getCell(0).getStringCellValue()).isIn("SALON", "Productos");
		}

		byte[] pdf = como("admin", get("/api/v1/reportes/margen/exportacion").param("fechaInicio", desde).param("fechaFin", hoy())
			.param("idSede", sedeDeLaClase().toString()).param("formato", "PDF"), null)
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/pdf"))
			.andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"reporte_margen_" + desde + "_" + hoy() + ".pdf\"")))
			.andReturn().getResponse().getContentAsByteArray();
		assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

		como("admin", get("/api/v1/reportes/ventas/exportacion").param("fechaInicio", hoy()).param("fechaFin", hoy()).param("formato", "XML"), null)
			.andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RF74"));
		comoNoAdministradora(get("/api/v1/reportes/productos/exportacion").param("fechaInicio", hoy()).param("fechaFin", hoy()).param("formato", "PDF"))
			.andExpect(status().isForbidden());
	}

}
