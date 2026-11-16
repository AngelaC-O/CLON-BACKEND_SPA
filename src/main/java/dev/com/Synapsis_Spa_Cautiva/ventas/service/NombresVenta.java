package dev.com.Synapsis_Spa_Cautiva.ventas.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.api.AtencionCobrable;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.CatalogoApi;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.api.DatosVariante;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.DatosProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.InventarioApi;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.PagosApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.UsuarioApi;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.DetalleVentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.PagoVentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.ProformaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.SolicitudAnulacionDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.controller.VentaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoItemVentaDetalle;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * [It. 2.1 · nombres] Los nombres que muestran las respuestas de ventas (página «02 Nombres en las respuestas»): la clienta (PersonaApi), la
 * descripción de cada línea (AtencionApi y CatalogoApi para «Servicio · Variante», InventarioApi para el producto, CatalogoApi para el paquete),
 * la especialista, el medio de pago (PagosApi) y los usuarios de una solicitud de anulación (UsuarioApi, D-31). Una llamada por tipo de nombre
 * para toda la respuesta. No está en los diagramas (DF-171).
 */
@Component
@RequiredArgsConstructor
public class NombresVenta {

	private final PersonaApi personaApi;

	private final AtencionApi atencionApi;

	private final CatalogoApi catalogoApi;

	private final InventarioApi inventarioApi;

	private final PagosApi pagosApi;

	private final UsuarioApi usuarioApi;

	public VentaDTO conNombres(VentaDTO oVentaDTO) {
		return conNombres(List.of(oVentaDTO)).getFirst();
	}

	/** nombreCliente de la venta, descripcion y nombreEspecialista de cada línea, nombreMedioPago de cada pago. */
	public List<VentaDTO> conNombres(List<VentaDTO> ventas) {
		if (ventas.isEmpty()) {
			return ventas;
		}
		List<DetalleVentaDTO> detalles = ventas.stream().flatMap(oVentaDTO -> oVentaDTO.detalles().stream()).toList();
		Map<Long, String> personas = personas(Stream.concat(ventas.stream().map(VentaDTO::idCliente), detalles.stream().map(DetalleVentaDTO::idPersonal))
			.toList());
		Map<Long, String> atenciones = ids(detalles.stream().map(DetalleVentaDTO::idAtencion).toList()).isEmpty() ? Map.of()
				: atencionApi.obtenerAtenciones(ids(detalles.stream().map(DetalleVentaDTO::idAtencion).toList())).stream()
					.collect(Collectors.toMap(AtencionCobrable::idAtencion, oAtencion -> Objects.requireNonNullElse(oAtencion.nombreServicio(), ""), (a, b) -> a));
		Map<Long, String> productos = productos(detalles.stream().map(DetalleVentaDTO::idProducto).toList());
		Map<Long, String> paquetes = paquetes(detalles.stream().map(DetalleVentaDTO::idPaquete).toList());
		List<Long> idsMedio = ids(ventas.stream().flatMap(oVentaDTO -> oVentaDTO.pagos().stream()).map(PagoVentaDTO::idMedioPago).toList());
		Map<Long, String> medios = idsMedio.isEmpty() ? Map.of() : pagosApi.nombresMediosPago(idsMedio);
		return ventas.stream()
			.map(oVentaDTO -> oVentaDTO.conNombres(de(personas, oVentaDTO.idCliente()),
					oVentaDTO.detalles().stream()
						.map(oDetalle -> oDetalle.conNombres(descripcion(oDetalle, atenciones, productos, paquetes), de(personas, oDetalle.idPersonal())))
						.toList(),
					oVentaDTO.pagos().stream().map(oPago -> oPago.conNombreMedioPago(de(medios, oPago.idMedioPago()))).toList()))
			.toList();
	}

	/** [It. 2.1 · D-31] La clienta y quién pidió y quién resolvió cada solicitud (Duda 14). */
	public List<SolicitudAnulacionDTO> conNombresDeSolicitudes(List<SolicitudAnulacionDTO> solicitudes) {
		if (solicitudes.isEmpty()) {
			return solicitudes;
		}
		Map<Long, String> personas = personas(solicitudes.stream().map(SolicitudAnulacionDTO::idCliente).toList());
		List<Long> idsUsuario = ids(solicitudes.stream()
			.flatMap(oSolicitud -> Stream.of(oSolicitud.idUsuarioSolicita(), oSolicitud.idUsuarioResuelve()))
			.toList());
		Map<Long, String> usuarios = idsUsuario.isEmpty() ? Map.of() : usuarioApi.nombresUsuarios(idsUsuario);
		return solicitudes.stream()
			.map(oSolicitud -> oSolicitud.conNombres(de(personas, oSolicitud.idCliente()), de(usuarios, oSolicitud.idUsuarioSolicita()),
					de(usuarios, oSolicitud.idUsuarioResuelve())))
			.toList();
	}

	public SolicitudAnulacionDTO conNombres(SolicitudAnulacionDTO oSolicitud) {
		return conNombresDeSolicitudes(List.of(oSolicitud)).getFirst();
	}

	/** [It. 2.1 · nombres] La descripción de cada ítem de la proforma: «Servicio · Variante» o el paquete. */
	public List<ProformaDTO.Item> conDescripciones(List<ProformaDTO.Item> items) {
		List<Long> idsVariante = ids(items.stream().map(ProformaDTO.Item::idVariante).toList());
		Map<Long, String> servicios = idsVariante.isEmpty() ? Map.of()
				: catalogoApi.obtenerVariantes(idsVariante).stream().collect(Collectors.toMap(DatosVariante::idVariante, DatosVariante::nombreServicio, (a, b) -> a));
		Map<Long, String> paquetes = paquetes(items.stream().map(ProformaDTO.Item::idPaquete).toList());
		return items.stream()
			.map(oItem -> oItem.conDescripcion(oItem.tipoItem() == TipoItemVentaDetalle.PAQUETE ? de(paquetes, oItem.idPaquete())
					: de(servicios, oItem.idVariante())))
			.toList();
	}

	/** idProducto → nombre, para la línea del CPE (D-33). */
	public Map<Long, String> productos(Collection<Long> idsProducto) {
		List<Long> ids = ids(idsProducto);
		return ids.isEmpty() ? Map.of()
				: inventarioApi.obtenerProductos(ids).stream().collect(Collectors.toMap(DatosProducto::idProducto, DatosProducto::nombre, (a, b) -> a));
	}

	private static String descripcion(DetalleVentaDTO oDetalle, Map<Long, String> atenciones, Map<Long, String> productos, Map<Long, String> paquetes) {
		if (oDetalle.tipoItem() == TipoItemVentaDetalle.PRODUCTO) {
			return de(productos, oDetalle.idProducto());
		}
		if (oDetalle.tipoItem() == TipoItemVentaDetalle.PAQUETE) {
			return de(paquetes, oDetalle.idPaquete());
		}
		return de(atenciones, oDetalle.idAtencion());
	}

	private Map<Long, String> personas(Collection<Long> idsPersona) {
		List<Long> ids = ids(idsPersona);
		return ids.isEmpty() ? Map.of()
				: personaApi.obtenerPersonas(ids).stream().collect(Collectors.toMap(PersonaDTO::idPersona, NombresVenta::nombreDe, (a, b) -> a));
	}

	private Map<Long, String> paquetes(Collection<Long> idsPaquete) {
		List<Long> ids = ids(idsPaquete);
		return ids.isEmpty() ? Map.of()
				: catalogoApi.obtenerPaquetes(ids).stream().collect(Collectors.toMap(DatosPaquete::idPaquete, DatosPaquete::nombre, (a, b) -> a));
	}

	private static String nombreDe(PersonaDTO oPersonaDTO) {
		return oPersonaDTO.apellidos() == null ? oPersonaDTO.nombres() : oPersonaDTO.nombres() + " " + oPersonaDTO.apellidos();
	}

	/** Map.of() no acepta get(null): un id nulo (clienta de paso, línea sin especialista) no tiene nombre. */
	private static String de(Map<Long, String> nombres, Long id) {
		return id == null ? null : nombres.get(id);
	}

	private static List<Long> ids(Collection<Long> ids) {
		return ids.stream().filter(Objects::nonNull).distinct().toList();
	}

}
