package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Size;

/**
 * [It. 2 · CUS65, RF87] Un parámetro del negocio. En el PATCH solo van valorNumero, valorTexto y fechaVigencia (sin fecha, rige desde hoy); el
 * código no se crea ni se cambia. idUsuario lo pone Oracle (quién lo cambió).
 */
public record ParametroDTO(Long idParametro, String codigo, String descripcion, BigDecimal valorNumero, @Size(max = 100) String valorTexto,
		LocalDate fechaVigencia, Long idUsuario) {
}
