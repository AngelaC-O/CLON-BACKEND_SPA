package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import java.math.BigDecimal;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoPaquete;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS59, RF84] Paquete o membresía con los servicios que incluye (PAQUETE_SERVICIO). Sesiones, precio y vigencia los valida el
 * constructor de Paquete (422). idPaquete y estado los completa el servidor. [It. 2.1 · nombres] servicios solo va en las respuestas, con el
 * nombre de cada servicio; en la petición se usa idsServicio (servicios se ignora).
 */
public record PaqueteDTO(Long idPaquete, @NotBlank @Size(max = 80) String nombre, @NotNull TipoPaquete tipo, Integer numSesiones,
		BigDecimal precio, Integer vigenciaDias, EstadoPaquete estado, List<Long> idsServicio, List<ServicioIncluidoDTO> servicios) {

	public PaqueteDTO conServicios(List<ServicioIncluidoDTO> servicios) {
		return new PaqueteDTO(idPaquete, nombre, tipo, numSesiones, precio, vigenciaDias, estado, idsServicio, servicios);
	}

}
