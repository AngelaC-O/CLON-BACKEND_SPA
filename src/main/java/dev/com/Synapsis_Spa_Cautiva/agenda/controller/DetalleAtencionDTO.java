package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import jakarta.validation.constraints.Size;

public record DetalleAtencionDTO(@Size(max = 300) String observaciones) {
}
