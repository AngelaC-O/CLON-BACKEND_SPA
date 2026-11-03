package dev.com.Synapsis_Spa_Cautiva.seguridad.api;

import java.time.LocalDateTime;

/**
 * resultado: ENCONTRADO, NO_ENCONTRADO o ERROR; con los dos últimos los datos se ingresan a mano (RN45).
 * fechaVerificacion: solo con ENCONTRADO; el formulario la devuelve al registrar para que la ficha quede VERIFICADO.
 */
public record ResultadoConsulta(Long idTipoDocumento, String resultado, String nombres, String apellidos, String razonSocial,
		LocalDateTime fechaVerificacion) {
}
