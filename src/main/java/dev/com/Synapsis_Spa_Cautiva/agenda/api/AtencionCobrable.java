package dev.com.Synapsis_Spa_Cautiva.agenda.api;

/** Lo que ventas necesita para cargar una atención al cobro; con idClientePaquete no se cobra ni comisiona (RN36, RN38). */
public record AtencionCobrable(Long idAtencion, Long idCliente, String nombreClientePaso, Long idPersonal, Long idVariante,
		Long idClientePaquete) {
}
