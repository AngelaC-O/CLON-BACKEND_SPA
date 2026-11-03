package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.time.LocalDateTime;

/** Una fila de V_AUDITORIA. */
public record RegistroAuditoria(Long idAuditoria, LocalDateTime fechaHora, String usuario, String rol, String modulo, String accion,
		String entidad, Integer idEntidad, String detalle) {
}
