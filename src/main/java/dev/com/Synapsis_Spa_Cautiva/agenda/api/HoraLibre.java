package dev.com.Synapsis_Spa_Cautiva.agenda.api;

import java.time.LocalDateTime;
import java.util.List;

/** Una hora con las especialistas y los recursos libres para la variante pedida (RF06, RF17). */
public record HoraLibre(LocalDateTime inicio, Integer duracionMin, List<Long> idsPersonal, List<Long> idsRecurso) {
}
