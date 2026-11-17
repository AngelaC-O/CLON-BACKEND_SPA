package dev.com.Synapsis_Spa_Cautiva.seguridad.api;

import java.util.List;
import java.util.Map;

/**
 * [It. 2.1 · nombres] Lo que seguridad publica de los usuarios para otros módulos: el nombre completo de la persona de cada usuario
 * (quién pidió o resolvió una anulación, D-31; quién abrió, cerró, revisó o reabrió una caja, Duda 25).
 */
public interface UsuarioApi {

	/** idUsuario → «nombres apellidos» de su persona, en lote (una consulta por lista). Los ids que no existen no vienen. */
	Map<Long, String> nombresUsuarios(List<Long> idsUsuario);

}
