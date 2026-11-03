package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

/** Las respuestas de los externos que los falsos no dan: cobro rechazado o pendiente, proveedor caído y comprobante aceptado. */
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
		when(pasarelaPagosPort.crearCobro(any())).thenReturn(new RespuestaPasarela("RECHAZADA", "RECH-1", null, "Fondos insuficientes"));

		pagar(idVenta, medio("TARJETA"), "35", "API").andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.regla").value("RF103"));

		assertThat(id("SELECT COUNT(*) FROM VENTA_PAGO WHERE id_venta = ?", idVenta)).isZero();
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
	}

}
