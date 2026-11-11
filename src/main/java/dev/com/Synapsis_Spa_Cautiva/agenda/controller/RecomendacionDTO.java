package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS12, RF16] Una recomendación de producto de la atención. En la petición solo van idProducto y observacion; idRecomendacion,
 * idAtencion y fecha los completa el servidor. También aparece en la ficha de la clienta (CUS03).
 */
public record RecomendacionDTO(Long idRecomendacion, Long idAtencion, @NotNull Long idProducto, @Size(max = 200) String observacion,
		LocalDateTime fecha) {
}
