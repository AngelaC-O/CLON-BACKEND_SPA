package dev.com.Synapsis_Spa_Cautiva.agenda.api;

import java.math.BigDecimal;

/**
 * Lo que ventas necesita para cargar una atención al cobro; con idClientePaquete no se cobra ni comisiona (RN36, RN38).
 * adelantoMonto: el de su cita, o 0 si no tiene (RF25). [It. 2.1 · nombres] nombreCliente (o el de la clienta de paso), nombreServicio
 * («Servicio · Variante») y nombreEspecialista: la lista «por cobrar» de CUS18 y la descripción del detalle de la venta.
 */
public record AtencionCobrable(Long idAtencion, Long idCliente, String nombreClientePaso, Long idPersonal, Long idVariante,
		Long idClientePaquete, BigDecimal adelantoMonto, String nombreCliente, String nombreServicio, String nombreEspecialista) {
}
