package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.caja.domain.Caja;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Persona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoPersona;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/** Reglas que validan las entidades antes de llegar a Oracle; no necesitan base de datos. */
class ReglasDominioTest {

	private void rechaza(String regla, ThrowingCallable accion) {
		assertThatThrownBy(accion).isInstanceOfSatisfying(ReglaNegocioException.class,
				ex -> assertThat(ex.getRegla()).isEqualTo(regla));
	}

	@Test
	void rn10NoSeCierraLaVentaSinComprobante() {
		Venta venta = new Venta(1L, 1L, null, 1L);
		rechaza("RN10", venta::cerrar);
	}

	@Test
	void rn11LosPagosDebenSumarElTotal() {
		Venta venta = new Venta(1L, 1L, null, 1L);
		venta.agregarServicio(1L, new BigDecimal("50.00"), 1L);
		venta.generarComprobante(TipoComprobante.BOLETA, null);
		venta.registrarPago(1L, new BigDecimal("20.00"), null);
		rechaza("RN11", venta::cerrar);

		venta.registrarPago(1L, new BigDecimal("30.00"), null);
		assertThatCode(venta::cerrar).doesNotThrowAnyException();
	}

	@Test
	void rn08LaFacturaExigeAlComprador() {
		Venta venta = new Venta(1L, 1L, null, 1L);
		rechaza("RN08", () -> venta.generarComprobante(TipoComprobante.FACTURA, null));
	}

	@Test
	void rn30NoSeCierraLaCajaSinArqueo() {
		Caja caja = new Caja(1L, 1L, BigDecimal.TEN);
		rechaza("RN30", () -> caja.cerrar(1L));
	}

	@Test
	void rn45SinDocumentoNoSeVerifica() {
		Persona persona = new Persona(TipoPersona.NATURAL, "Ana", null, null, null, "987654321", null);
		rechaza("RN45", () -> persona.marcarVerificado(LocalDateTime.now()));
	}

}
