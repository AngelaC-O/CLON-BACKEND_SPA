package dev.com.Synapsis_Spa_Cautiva.seguridad.controller;

import java.time.LocalDateTime;

/** [It. 2 · CUS55, RF80] Una fila de V_AUDITORIA: quién (usuario y rol), cuándo, qué módulo, qué acción, sobre qué dato y su detalle. */
public record AuditoriaDTO(Long idAuditoria, LocalDateTime fechaHora, String usuario, String rol, String modulo, String accion, String entidad,
		Integer idEntidad, String detalle) {
}
