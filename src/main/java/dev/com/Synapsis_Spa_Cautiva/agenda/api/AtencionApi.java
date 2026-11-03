package dev.com.Synapsis_Spa_Cautiva.agenda.api;

import java.util.List;

/** Lo que ventas usa de las atenciones (CUS13, CUS18). */
public interface AtencionApi {

	List<HoraLibre> libresAhora(Long idVariante, Long idSede);

	Long registrarSinCita(Long idCliente, String nombreClientePaso, Long idPersonal, Long idRecurso, Long idVariante, Integer tiempoDeclaradoMin);

	/** CUS13 cobra en el acto, y Oracle solo cobra una atención FINALIZADA: su fin es el tiempo declarado, para que siga ocupando la agenda (DF-37). */
	void finalizarSinCita(Long idAtencion);

	/** La descripción del comprobante electrónico: la línea de venta no guarda la variante (CK_VENTA_DETALLE_ITEM). */
	Long obtenerIdVariante(Long idAtencion);

	/** 409 si ya se cobró; 422 si aún no termina (RF21). */
	AtencionCobrable obtenerAtencionCobrable(Long idAtencion);

}
