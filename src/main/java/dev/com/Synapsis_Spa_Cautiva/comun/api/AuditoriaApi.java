package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SPI de la auditoría (ADR-08). Los cambios de datos los registran los triggers TRG_AUD_*: registrar es solo para lo que
 * no cambia una tabla, como un acceso denegado.
 */
public interface AuditoriaApi {

	void registrar(Long idUsuario, String accion, String entidad, Integer idEntidad, String detalle);

	List<RegistroAuditoria> consultar(LocalDateTime desde, LocalDateTime hasta);

}
