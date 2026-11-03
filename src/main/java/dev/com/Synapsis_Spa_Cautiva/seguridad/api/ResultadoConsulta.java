package dev.com.Synapsis_Spa_Cautiva.seguridad.api;

/** resultado: ENCONTRADO, NO_ENCONTRADO o ERROR; con los dos últimos los datos se ingresan a mano (RN45). */
public record ResultadoConsulta(Long idTipoDocumento, String resultado, String nombres, String apellidos, String razonSocial) {
}
