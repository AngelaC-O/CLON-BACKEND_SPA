package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.com.Synapsis_Spa_Cautiva.pagos.service.PasarelaPagosPort;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.RespuestaPasarela;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSunat;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.FacturacionElectronicaPort;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.RespuestaCpe;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Las respuestas de los externos que los falsos no dan: cobro rechazado o pendiente, proveedor caído y comprobante aceptado. [It. 2] También
 * los GET de CUS18, CUS20 y CUS44 modificados y la venta rápida con cobro PENDIENTE (RN52); esta clase no cierra su caja.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasarelaYFacturacionFlujoTest extends VentasFlujoApoyo {

	@MockitoBean
	private PasarelaPagosPort pasarelaPagosPort;

	@MockitoBean
	private FacturacionElectronicaPort facturacionElectronicaPort;

	private Long ventaAbierta() throws Exception {
		cajaAbierta();
		return idDe(crearVenta(atencionFinalizada(null, variante(CORTE))).andExpect(status().isCreated()), "idVenta");
	}

	@Test
	void cus18UnCobroRechazadoEs422YNoRegistraElPago() throws Exception {
		Long idVenta = ventaAbierta();
		when(pasarelaPagosPort.crearCobro(any())).thenReturn(new RespuestaPasarela("RECHAZADA", "RECH-" + idVenta, null, "Fondos insuficientes"));

		pagar(idVenta, medio("TARJETA"), "35", "API").andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RF103"));

		assertThat(id("SELECT COUNT(*) FROM VENTA_PAGO WHERE id_venta = ?", idVenta)).isZero();
		// [It. 2 · RN52] La transacción RECHAZADA se guardó aparte y queda aunque el pedido hizo rollback
		assertThat(id("SELECT COUNT(*) FROM TRANSACCION_PASARELA WHERE codigo_operacion = ? AND estado = 'RECHAZADA'", "RECH-" + idVenta)).isEqualTo(1);
	}

	@Test
	void cus18UnCobroPendienteEs202YNoRegistraElPago() throws Exception {
		Long idVenta = ventaAbierta();
		when(pasarelaPagosPort.crearCobro(any())).thenReturn(new RespuestaPasarela("PENDIENTE", "PEND-" + idVenta, null, "En proceso"));

		pagar(idVenta, medio("YAPE"), "35", "QR").andExpect(status().isAccepted())
			.andExpect(jsonPath("$.cobrosPendientes", hasSize(1)))
			.andExpect(jsonPath("$.pagos", hasSize(0)))
			.andExpect(jsonPath("$.pendiente").value(35.0));

		assertThat(id("SELECT COUNT(*) FROM TRANSACCION_PASARELA WHERE codigo_operacion = ? AND estado = 'PENDIENTE'", "PEND-" + idVenta)).isEqualTo(1);
	}

	@Test
	void cus13UnaVentaRapidaConPagoPendienteEs202YLaVentaQuedaAbierta() throws Exception {
		cajaAbierta();
		String codigo = "PEND-RAPIDA-" + System.nanoTime();
		when(pasarelaPagosPort.crearCobro(any())).thenReturn(new RespuestaPasarela("PENDIENTE", codigo, "https://pasarela-falsa.invalid/qr", "En proceso"));
		Long atencionesAntes = id("SELECT COUNT(*) FROM ATENCION");
		String cuerpo = "{\"nombreClientePaso\":\"De paso con Yape\",\"idPersonal\":" + especialista() + ",\"idRecurso\":" + recurso(1) + ",\"idVariante\":"
				+ variante(CORTE) + ",\"tiempoDeclaradoMin\":30,\"idMedioPago\":" + medio("YAPE") + ",\"monto\":35,\"canal\":\"QR\",\"tipo\":\"BOLETA\"}";

		ResultActions resultado = enviar(post("/api/v1/ventas/rapidas"), cuerpo).andExpect(status().isAccepted())
			.andExpect(jsonPath("$.estado").value("ABIERTA"))
			.andExpect(jsonPath("$.pagos", hasSize(0)))
			.andExpect(jsonPath("$.cobrosPendientes", hasSize(1)))
			.andExpect(jsonPath("$.cobrosPendientes[0].enlacePago").value("https://pasarela-falsa.invalid/qr"))
			.andExpect(jsonPath("$.comprobante").doesNotExist());
		Long idVenta = idDe(resultado, "idVenta");
		resultado.andExpect(header().string("Location", "/api/v1/ventas/" + idVenta));

		// RN52: la atención y la venta quedan; la venta sigue ABIERTA y el cobro, PENDIENTE
		assertThat(id("SELECT COUNT(*) FROM ATENCION")).isEqualTo(atencionesAntes + 1);
		assertThat(jdbc.queryForObject("SELECT estado FROM VENTA WHERE id_venta = ?", String.class, idVenta)).isEqualTo("ABIERTA");
		assertThat(id("SELECT COUNT(*) FROM TRANSACCION_PASARELA WHERE codigo_operacion = ? AND estado = 'PENDIENTE'", codigo)).isEqualTo(1);
		enviar(get("/api/v1/ventas/" + idVenta), null).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ABIERTA"));
		enviar(get("/api/v1/ventas/por-cobrar"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.ventasAbiertas[*].idVenta", hasItem(idVenta.intValue())));
	}

	@Test
	void cus18PorCobrarTraeLasAtencionesSinVentaYLaVentaConSusCobros() throws Exception {
		cajaAbierta();
		Long idAtencion = atencionFinalizada(null, variante(CORTE));
		enviar(get("/api/v1/ventas/por-cobrar"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.atenciones[*].idAtencion", hasItem(idAtencion.intValue())));

		Long idVenta = idDe(crearVenta(idAtencion).andExpect(status().isCreated()), "idVenta");
		enviar(get("/api/v1/ventas/por-cobrar"), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.atenciones[*].idAtencion", not(hasItem(idAtencion.intValue()))))
			.andExpect(jsonPath("$.ventasAbiertas[*].idVenta", hasItem(idVenta.intValue())));

		when(pasarelaPagosPort.crearCobro(any())).thenReturn(new RespuestaPasarela("APROBADA", "APR-" + idVenta, null, "Aprobada"));
		pagar(idVenta, medio("TARJETA"), "35", "POS").andExpect(status().isOk());
		enviar(get("/api/v1/ventas/" + idVenta), null).andExpect(status().isOk())
			.andExpect(jsonPath("$.pagos[0].estadoTransaccion").value("APROBADA"))
			.andExpect(jsonPath("$.pendiente").value(0.0));
		enviar(get("/api/v1/ventas/999999"), null).andExpect(status().isNotFound());
	}

	@Test
	void cus44LaCajaAbiertaEs200O204SinCaja() throws Exception {
		Long idCaja = cajaAbierta();
		enviar(get("/api/v1/cajas/abierta"), null).andExpect(status().isOk()).andExpect(jsonPath("$.idCaja").value(idCaja))
			.andExpect(jsonPath("$.estado").value("ABIERTA"));
		enviarEnSede("recepcion", sedeNueva("Sin caja"), get("/api/v1/cajas/abierta"), null).andExpect(status().isNoContent());
	}

	@Test
	void cus19SiElProveedorFallaLaVentaSeCierraConElComprobantePendiente() throws Exception {
		when(facturacionElectronicaPort.emitir(any())).thenThrow(new IllegalStateException("APISPERU no responde"));

		ventaCerrada().andExpect(jsonPath("$.estado").value("CERRADA")).andExpect(jsonPath("$.comprobante.estadoSunat").value("PENDIENTE"));
	}

	@Test
	void cus19YCus20GuardanElXmlDelComprobanteAceptadoYLoEntregan() throws Exception {
		when(facturacionElectronicaPort.emitir(any()))
			.thenReturn(new RespuestaCpe(EstadoSunat.ACEPTADO, "0", "Aceptado", "HASH123", "<xml>comprobante</xml>", "<xml>cdr</xml>"));

		Long idComprobante = idDe(ventaCerrada().andExpect(jsonPath("$.comprobante.estadoSunat").value("ACEPTADO")).andExpect(jsonPath("$.comprobante.codigoRespuesta").value("0")),
				"comprobante.idComprobante");

		assertThat(jdbc.queryForObject("SELECT hash_cpe FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idComprobante)).isEqualTo("HASH123");
		assertThat(jdbc.queryForObject("SELECT ruta_xml FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idComprobante)).endsWith(".xml");
		assertThat(jdbc.queryForObject("SELECT ruta_cdr FROM COMPROBANTE WHERE id_comprobante = ?", String.class, idComprobante)).endsWith("-cdr.xml");
		// [It. 2] CUS20: el estado SUNAT actual del comprobante
		enviar(get("/api/v1/comprobantes/" + idComprobante), null).andExpect(status().isOk()).andExpect(jsonPath("$.estadoSunat").value("ACEPTADO"))
			.andExpect(jsonPath("$.idComprobante").value(idComprobante));
		enviar(get("/api/v1/comprobantes/999999"), null).andExpect(status().isNotFound());
	}

}
