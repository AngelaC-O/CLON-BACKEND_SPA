package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.comun.api.CajaAbiertaProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.AdelantoAprobado;
import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Registra el adelanto digital en la transacción del webhook; TRG_CITA_ADELANTO exige el cobro APROBADO (RN04). Sin caja abierta queda sin
 * caja: el cobro ya se hizo (DF-40).
 */
@Service
@RequiredArgsConstructor
public class ManejadorAdelantoConfirmado {

	private final RepositorioCita repositorioCita;

	private final CajaAbiertaProvider cajaAbiertaProvider;

	@EventListener
	public void on(AdelantoAprobado oAdelantoAprobado) {
		Cita oCita = repositorioCita.findById(oAdelantoAprobado.idCita())
			.orElseThrow(() -> new ReglaNegocioException("RN04", "La cita " + oAdelantoAprobado.idCita() + " no existe.", HttpStatus.NOT_FOUND));
		Long idCaja = cajaAbiertaProvider.buscarCajaAbierta(oCita.getIdSede()).orElse(null);
		oCita.registrarAdelanto(oAdelantoAprobado.monto(), oAdelantoAprobado.idMedioPago(), LocalDateTime.now(), idCaja);
		repositorioCita.save(oCita);
	}

}
