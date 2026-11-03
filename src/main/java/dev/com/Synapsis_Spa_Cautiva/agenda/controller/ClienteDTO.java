package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ficha de la clienta: los datos comunes son de PERSONA (PersonaApi) y el resto de CLIENTE. */
public record ClienteDTO(Long idCliente, Long idTipoDocumento, @Size(max = 15) String numeroDocumento, @NotBlank @Size(max = 80) String nombres,
		@Size(max = 80) String apellidos, @NotBlank @Size(max = 15) String telefono, @Email @Size(max = 100) String correo, String origenDatos,
		LocalDateTime fechaVerificacion, LocalDate fechaCumpleanos, @NotNull Boolean aceptaComunicaciones, LocalDateTime fechaConsentimiento,
		@Size(max = 300) String observaciones, EstadoCliente estado) {
}
