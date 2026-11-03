package dev.com.Synapsis_Spa_Cautiva.pagos.controller;

import dev.com.Synapsis_Spa_Cautiva.pagos.service.NotificacionPasarela;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.PasarelaPagosPort;
import dev.com.Synapsis_Spa_Cautiva.pagos.service.ServicioPago;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Webhook de Izipay: sin sesión ni CSRF, lo valida la firma. */
@RestController
@RequestMapping("/api/v1/pagos/notificaciones")
@RequiredArgsConstructor
public class ControladorNotificacionPasarela {

	private final ServicioPago servicioPago;

	private final PasarelaPagosPort pasarelaPagosPort;

	@PostMapping
	public ResponseEntity<Void> recibir(@RequestBody String cuerpo, @RequestHeader(name = "X-Firma-Hmac", required = false) String firmaHmac) {
		NotificacionPasarela oNotificacion = pasarelaPagosPort.traducirNotificacion(cuerpo, firmaHmac);
		servicioPago.confirmar(oNotificacion.codigoOperacion(), oNotificacion.estado());
		return ResponseEntity.ok().build();
	}

}
