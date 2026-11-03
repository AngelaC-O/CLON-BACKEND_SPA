package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.math.BigDecimal;

import dev.com.Synapsis_Spa_Cautiva.ventas.controller.ProformaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.VentaDetalle;

import org.springframework.stereotype.Service;

/** Total sin IGV (RN06), menos adelanto y saldo a favor (RF25). */
@Service
public class CalculadoraVenta {

	public BigDecimal calcularTotal(Venta oVenta) {
		return oVenta.getDetalles().stream()
			.map(VentaDetalle::getSubtotal)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.subtract(oVenta.getDescuento())
			.subtract(oVenta.getAdelantoAplicado())
			.subtract(oVenta.getSaldoFavorAplicado());
	}

	public BigDecimal calcularTotal(ProformaDTO oProformaDTO) {
		return oProformaDTO.items().stream().map(ProformaDTO.Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

}
