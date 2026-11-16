package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosVariante;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.DatosProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.InventarioApi;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.DatosRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.api.RecursoApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * [It. 2.1 · nombres] Los nombres que las respuestas de agenda muestran en lugar de los id (página «02 Nombres en las respuestas»): personas
 * (PersonaApi), «Servicio · Variante» (CatalogoApi), recursos (RecursoApi), productos (InventarioApi) y paquetes (CatalogoApi). Cada tipo se busca
 * con una sola llamada por respuesta, nunca una por fila. No está en los diagramas: la página dice que los mensajes que buscan los nombres no
 * se dibujan en cada CUS (DF-171).
 */
@Component
@RequiredArgsConstructor
public class NombresAgenda {

	private final PersonaApi personaApi;

	private final CatalogoApi catalogoApi;

	private final RecursoApi recursoApi;

	private final InventarioApi inventarioApi;

	private final UsuarioActual usuarioActual;

	/** Los nombres de una respuesta. Cualquier colección puede venir vacía; los null se ignoran. */
	public Nombres buscar(Collection<Long> idsPersona, Collection<Long> idsVariante, Collection<Long> idsRecurso, Collection<Long> idsProducto,
			Collection<Long> idsPaquete) {
		return new Nombres(personas(idsPersona), servicios(idsVariante), recursos(idsRecurso), productos(idsProducto), paquetes(idsPaquete));
	}

	/** «nombres apellidos» de la persona; sin apellidos, solo los nombres. */
	public static String nombreDe(PersonaDTO oPersonaDTO) {
		if (oPersonaDTO == null) {
			return null;
		}
		return oPersonaDTO.apellidos() == null ? oPersonaDTO.nombres() : oPersonaDTO.nombres() + " " + oPersonaDTO.apellidos();
	}

	private Map<Long, String> personas(Collection<Long> ids) {
		List<Long> lista = distintos(ids);
		return lista.isEmpty() ? Map.of()
				: personaApi.obtenerPersonas(lista).stream().collect(Collectors.toMap(PersonaDTO::idPersona, NombresAgenda::nombreDe, (a, b) -> a));
	}

	private Map<Long, String> servicios(Collection<Long> ids) {
		List<Long> lista = distintos(ids);
		return lista.isEmpty() ? Map.of()
				: catalogoApi.obtenerVariantes(lista).stream().collect(Collectors.toMap(DatosVariante::idVariante, DatosVariante::nombreServicio, (a, b) -> a));
	}

	/**
	 * Los recursos de la sede de la sesión, en una llamada (RecursoApi.listarRecursos, como dice la página). Un recurso de otra sede, que no
	 * debería aparecer, se busca aparte por su id (DF-172).
	 */
	private Map<Long, String> recursos(Collection<Long> ids) {
		List<Long> lista = distintos(ids);
		if (lista.isEmpty()) {
			return Map.of();
		}
		Map<Long, String> nombres = new HashMap<>(recursoApi.listarRecursos(usuarioActual.idSede()).stream()
			.filter(oRecurso -> lista.contains(oRecurso.idRecurso()))
			.collect(Collectors.toMap(DatosRecurso::idRecurso, DatosRecurso::nombre)));
		for (Long idRecurso : lista) {
			if (!nombres.containsKey(idRecurso)) {
				try {
					nombres.put(idRecurso, recursoApi.obtenerRecurso(idRecurso).nombre());
				}
				catch (ReglaNegocioException ex) {
					// el recurso no existe: la respuesta sigue sin su nombre
				}
			}
		}
		return nombres;
	}

	private Map<Long, String> productos(Collection<Long> ids) {
		List<Long> lista = distintos(ids);
		return lista.isEmpty() ? Map.of()
				: inventarioApi.obtenerProductos(lista).stream().collect(Collectors.toMap(DatosProducto::idProducto, DatosProducto::nombre, (a, b) -> a));
	}

	private Map<Long, String> paquetes(Collection<Long> ids) {
		List<Long> lista = distintos(ids);
		return lista.isEmpty() ? Map.of()
				: catalogoApi.obtenerPaquetes(lista).stream().collect(Collectors.toMap(DatosPaquete::idPaquete, DatosPaquete::nombre, (a, b) -> a));
	}

	private static List<Long> distintos(Collection<Long> ids) {
		return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
	}

	/** Los nombres buscados para una respuesta; un id sin nombre (o null) da null. */
	public record Nombres(Map<Long, String> personas, Map<Long, String> servicios, Map<Long, String> recursos, Map<Long, String> productos,
			Map<Long, String> paquetes) {

		public String persona(Long id) {
			return id == null ? null : personas.get(id);
		}

		/** «Servicio · Variante». */
		public String servicio(Long idVariante) {
			return idVariante == null ? null : servicios.get(idVariante);
		}

		public String recurso(Long id) {
			return id == null ? null : recursos.get(id);
		}

		public String producto(Long id) {
			return id == null ? null : productos.get(id);
		}

		public String paquete(Long id) {
			return id == null ? null : paquetes.get(id);
		}

		/** La clienta registrada o, si es de paso, el nombre que se anotó (RN35). */
		public String cliente(Long idCliente, String nombreClientePaso) {
			return idCliente == null ? nombreClientePaso : persona(idCliente);
		}

	}

}
