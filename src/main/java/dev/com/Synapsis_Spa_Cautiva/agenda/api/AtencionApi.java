package dev.com.Synapsis_Spa_Cautiva.agenda.api;

import java.util.List;

/** Lo que ventas usa de las atenciones (CUS13, CUS18). */
public interface AtencionApi {

	List<HoraLibre> libresAhora(Long idVariante, Long idSede);

	Long registrarSinCita(Long idCliente, String nombreClientePaso, Long idPersonal, Long idRecurso, Long idVariante, Integer tiempoDeclaradoMin);

	AtencionCobrable obtenerAtencionCobrable(Long idAtencion);

}
