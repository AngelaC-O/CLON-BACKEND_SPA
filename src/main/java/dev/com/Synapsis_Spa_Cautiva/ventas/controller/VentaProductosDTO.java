package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS26, RF36] Venta de productos: clienta opcional, las líneas con la especialista que recomendó cada producto (RF38), un pago y el
 * comprobante, como en la pantalla del diagrama. canal: solo para los pagos digitales.
 */
public record VentaProductosDTO(Long idCliente, @NotEmpty List<@Valid Linea> lineas, @NotNull Long idMedioPago, @NotNull @Positive BigDecimal monto,
		@Pattern(regexp = "POS|QR|LINK|API") String canal, @NotNull TipoComprobante tipo, Long idTipoDocComprador, @Size(max = 15) String nroDocComprador,
		@Size(max = 150) String nombreComprador) {

	/** idPersonal: la especialista que recomendó el producto (VENTA_DETALLE.id_personal, RF38); los productos no comisionan (RN38). */
	public record Linea(@NotNull Long idProducto, @NotNull @Positive Integer cantidad, Long idPersonal) {
	}

}
