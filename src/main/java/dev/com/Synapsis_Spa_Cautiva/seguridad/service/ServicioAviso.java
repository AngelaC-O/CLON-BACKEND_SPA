package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.util.Collection;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoDTO;
import dev.com.Synapsis_Spa_Cautiva.comun.api.AvisoProvider;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * [It. 2] Junta los avisos de todos los AvisoProvider (inventario, ventas y caja agregan el suyo en sus tareas) y deja solo los que el rol de
 * la sesión puede ver. Sin proveedores, la lista sale vacía. Cada proveedor maneja su propia transacción.
 */
@Service
@RequiredArgsConstructor
public class ServicioAviso {

	private final ObjectProvider<AvisoProvider> avisoProviders;

	public List<AvisoDTO> listarAvisos(Long idSede, Collection<String> permisos) {
		return avisoProviders.orderedStream()
			.flatMap(oAvisoProvider -> oAvisoProvider.avisos(idSede).stream())
			.filter(oAviso -> oAviso.permisoRequerido() == null || permisos.contains(oAviso.permisoRequerido()))
			.toList();
	}

}
