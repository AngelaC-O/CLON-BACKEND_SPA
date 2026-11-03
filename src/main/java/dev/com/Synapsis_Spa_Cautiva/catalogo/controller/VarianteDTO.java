package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VarianteDTO(Long idVariante, @NotBlank @Size(max = 60) String nombreVariante, @NotNull BigDecimal precio,
		List<@Valid InsumoDTO> insumos) {
}
