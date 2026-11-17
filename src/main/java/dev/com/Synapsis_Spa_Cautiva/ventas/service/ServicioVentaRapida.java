package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.CierreVentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.PagosDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.VentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.VentaRapidaDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ServicioVentaRapida {

	private final AtencionApi atencionApi;

	private final ServicioVenta servicioVenta;

	private final PagosApi pagosApi;

	private final TransactionTemplate transacciones;

	public List<HoraLibre> libresAhora(Long idVariante, Long idSede) {
		return atencionApi.libresAhora(idVariante, idSede);
	}

	/**
	 * Todo en una transacción: si falla el cobro o el cierre, no queda una atención huérfana (RF20). [It. 2 · RN52] Con un pago digital que la
	 * pasarela aún no aprueba, la atención y la venta quedan guardadas, la venta sigue ABIERTA y no se cierra: vuelve con cobrosPendientes y el
	 * controlador responde 202. Cuando el webhook lo aprueba, la recepcionista registra el pago con idTransaccion y cierra la venta (CUS18).
	 * [It. 2.1 · D-22] Con un medio de la pasarela, la atención y la venta se confirman antes de cobrar (con RN11 revisada antes), porque el
	 * cobro guarda id_venta desde su propia transacción y Oracle busca la venta ABIERTA (TRG_TRANS_PAS_REGLAS). Si la pasarela rechaza (422), la
	 * venta queda ABIERTA sin pago y se retoma o se descarta en CUS18 (DF-162). En efectivo, todo va en una transacción, como antes.
	 */
	public VentaDTO registrarYCobrar(VentaRapidaDTO oVentaRapidaDTO) {
		if (!pagosApi.usaPasarela(oVentaRapidaDTO.idMedioPago())) {
			return transacciones.execute(estado -> {
				Long idVenta = servicioVenta.crearDesdeAtencion(registrarAtencion(oVentaRapidaDTO)).idVenta();
				servicioVenta.registrarPagos(idVenta, pagos(oVentaRapidaDTO));
				return servicioVenta.cerrar(idVenta, cierre(oVentaRapidaDTO));
			});
		}
		Long idVenta = transacciones.execute(estado -> {
			VentaDTO oVentaDTO = servicioVenta.crearDesdeAtencion(registrarAtencion(oVentaRapidaDTO));
			// RN11 antes de confirmar: un monto distinto del total no deja una atención ni una venta guardadas
			if (oVentaRapidaDTO.monto().compareTo(oVentaDTO.total()) != 0) {
				throw new ReglaNegocioException("RN11", "El pago es de S/ " + oVentaRapidaDTO.monto() + " y el total es S/ " + oVentaDTO.total() + ".");
			}
			return oVentaDTO.idVenta();
		});
		VentaDTO oVentaDTO = servicioVenta.registrarPagos(idVenta, pagos(oVentaRapidaDTO));
		if (!oVentaDTO.cobrosPendientes().isEmpty()) {
			return oVentaDTO;
		}
		return servicioVenta.cerrar(idVenta, cierre(oVentaRapidaDTO));
	}

	private Long registrarAtencion(VentaRapidaDTO oVentaRapidaDTO) {
		Long idAtencion = atencionApi.registrarSinCita(oVentaRapidaDTO.idCliente(), oVentaRapidaDTO.nombreClientePaso(), oVentaRapidaDTO.idPersonal(),
				oVentaRapidaDTO.idRecurso(), oVentaRapidaDTO.idVariante(), oVentaRapidaDTO.tiempoDeclaradoMin());
		atencionApi.finalizarSinCita(idAtencion);
		return idAtencion;
	}

	private static PagosDTO pagos(VentaRapidaDTO oVentaRapidaDTO) {
		return new PagosDTO(List.of(new PagosDTO.Pago(oVentaRapidaDTO.idMedioPago(), oVentaRapidaDTO.monto(), oVentaRapidaDTO.canal(), null)));
	}

	private static CierreVentaDTO cierre(VentaRapidaDTO oVentaRapidaDTO) {
		return new CierreVentaDTO(oVentaRapidaDTO.tipo(), oVentaRapidaDTO.idTipoDocComprador(), oVentaRapidaDTO.nroDocComprador(),
				oVentaRapidaDTO.nombreComprador());
	}

}
