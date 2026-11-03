package dev.com.Synapsis_Spa_Cautiva.agenda.api;

import java.math.BigDecimal;

/**
 * Lo que ventas necesita para cargar una atención al cobro; con idClientePaquete no se cobra ni comisiona (RN36, RN38).
 * adelantoMonto: el de su cita, o 0 si no tiene (RF25).
 */
public record AtencionCobrable(Long idAtencion, Long idCliente, String nombreClientePaso, Long idPersonal, Long idVariante,
		Long idClientePaquete, BigDecimal adelantoMonto) {
}
