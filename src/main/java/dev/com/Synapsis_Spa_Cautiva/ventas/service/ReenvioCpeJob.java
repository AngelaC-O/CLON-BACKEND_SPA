package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Reenvía los comprobantes PENDIENTES y alerta los cercanos a CPE_PLAZO_ENVIO_DIAS (RF101, RN46). Lo dispara el reloj: no tiene usuario. */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
class ReenvioCpeJob {

	private final ServicioFacturacionElectronica servicioFacturacionElectronica;

	@Scheduled(cron = "${cautiva.comprobantes.reenvio-cron}", zone = "America/Lima")
	void reenviar() {
		log.info("Comprobantes reenviados: {}", servicioFacturacionElectronica.reenviarPendientes());
	}

}
