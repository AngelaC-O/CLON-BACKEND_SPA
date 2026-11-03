package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PagosDTO(@NotEmpty List<@Valid Pago> pagos) {

	/** canal: solo para los pagos digitales. idTransaccion: el cobro que quedó pendiente y ya se aprobó; con él no se cobra otra vez (DF-51). */
	public record Pago(@NotNull Long idMedioPago, @NotNull @Positive BigDecimal monto, @Pattern(regexp = "POS|QR|LINK|API") String canal, Long idTransaccion) {
	}

}
