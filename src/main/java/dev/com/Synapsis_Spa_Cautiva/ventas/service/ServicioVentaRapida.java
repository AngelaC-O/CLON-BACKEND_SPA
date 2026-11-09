package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.CierreVentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.PagosDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.VentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.VentaRapidaDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioVentaRapida {

	private final AtencionApi atencionApi;

	private final ServicioVenta servicioVenta;

	public List<HoraLibre> libresAhora(Long idVariante, Long idSede) {
		return atencionApi.libresAhora(idVariante, idSede);
	}

	/**
	 * Todo en una transacción: si falla el cobro o el cierre, no queda una atención huérfana (RF20). [It. 2 · RN52] Con un pago digital que la
	 * pasarela aún no aprueba, la atención y la venta quedan guardadas, la venta sigue ABIERTA y no se cierra: vuelve con cobrosPendientes y el
	 * controlador responde 202. Cuando el webhook lo aprueba, la recepcionista registra el pago con idTransaccion y cierra la venta (CUS18).
	 */
	@Transactional
	public VentaDTO registrarYCobrar(VentaRapidaDTO oVentaRapidaDTO) {
		Long idAtencion = atencionApi.registrarSinCita(oVentaRapidaDTO.idCliente(), oVentaRapidaDTO.nombreClientePaso(), oVentaRapidaDTO.idPersonal(),
				oVentaRapidaDTO.idRecurso(), oVentaRapidaDTO.idVariante(), oVentaRapidaDTO.tiempoDeclaradoMin());
		atencionApi.finalizarSinCita(idAtencion);
		Long idVenta = servicioVenta.crearDesdeAtencion(idAtencion).idVenta();
		VentaDTO oVentaDTO = servicioVenta.registrarPagos(idVenta,
				new PagosDTO(List.of(new PagosDTO.Pago(oVentaRapidaDTO.idMedioPago(), oVentaRapidaDTO.monto(), oVentaRapidaDTO.canal(), null))));
		if (!oVentaDTO.cobrosPendientes().isEmpty()) {
			return oVentaDTO;
		}
		return servicioVenta.cerrar(idVenta, new CierreVentaDTO(oVentaRapidaDTO.tipo(), oVentaRapidaDTO.idTipoDocComprador(),
				oVentaRapidaDTO.nroDocComprador(), oVentaRapidaDTO.nombreComprador()));
	}

}
