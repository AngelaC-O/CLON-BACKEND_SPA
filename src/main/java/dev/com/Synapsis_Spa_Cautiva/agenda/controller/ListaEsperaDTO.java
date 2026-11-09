package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.HoraLibre;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoListaEspera;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * horasLibres: otras horas para ofrecerle a la clienta antes de dejarla en espera (RF09). [It. 2] nombreCliente y nombreServicio solo los
 * llena GET /citas/lista-espera (CUS06), con PersonaApi y CatalogoApi.
 */
public record ListaEsperaDTO(Long idEspera, @NotNull Long idCliente, Long idSede, @NotNull Long idVariante, @NotNull LocalDate fechaDeseada,
		@Pattern(regexp = HORA) String horaDesde, @Pattern(regexp = HORA) String horaHasta, EstadoListaEspera estado, Long idCita, Long idUsuario,
		List<HoraLibre> horasLibres, String nombreCliente, String nombreServicio) {

	private static final String HORA = "^([01][0-9]|2[0-3]):[0-5][0-9]$";

}
