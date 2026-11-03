package dev.com.Synapsis_Spa_Cautiva.seguridad.repository;

import java.time.LocalDateTime;

/** Proyección de V_AUDITORIA, que no tiene entidad. */
public interface FilaAuditoria {

	Long getIdAuditoria();

	LocalDateTime getFechaHora();

	String getUsuario();

	String getRol();

	String getModulo();

	String getAccion();

	String getEntidad();

	Integer getIdEntidad();

	String getDetalle();

}
