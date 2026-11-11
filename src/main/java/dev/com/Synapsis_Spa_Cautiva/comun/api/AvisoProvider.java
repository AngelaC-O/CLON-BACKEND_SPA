package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.util.List;

/**
 * [It. 2] SPI: los avisos que un módulo publica para el panel de Inicio (CUS50). Cada módulo lo implementa con lo suyo (inventario, ventas,
 * caja) y seguridad los junta en GET /avisos (ServicioAviso), que filtra por los permisos del rol.
 */
public interface AvisoProvider {

	List<AvisoDTO> avisos(Long idSede);

}
