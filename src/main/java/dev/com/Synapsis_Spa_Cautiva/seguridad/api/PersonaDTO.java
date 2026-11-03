package dev.com.Synapsis_Spa_Cautiva.seguridad.api;

import java.time.LocalDateTime;

/** tipoPersona: NATURAL o JURIDICA; origenDatos: VERIFICADO o MANUAL. Son texto porque los enums del dominio son internos. */
public record PersonaDTO(Long idPersona, String tipoPersona, String nombres, String apellidos, String razonSocial, Long idTipoDocumento,
		String numeroDocumento, String telefono, String correo, String origenDatos, LocalDateTime fechaVerificacion) {
}
