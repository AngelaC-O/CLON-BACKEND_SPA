package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

/** Respuesta del servicio externo de consulta DNI/RUC. resultado: ENCONTRADO, NO_ENCONTRADO o ERROR. */
public record DatosPersona(String resultado, String nombres, String apellidos, String razonSocial) {
}
