package dev.com.Synapsis_Spa_Cautiva.ventas.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.ProductoStockDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.ResultadoConsulta;
import dev.com.Synapsis_Spa_Cautiva.ventas.service.ServicioVenta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ControladorVenta {

	private static final String ENCONTRADO = "ENCONTRADO";

	private final ServicioVenta servicioVenta;

	private final UsuarioActual usuarioActual;

	/** [It. 2] CUS18: atenciones FINALIZADAS sin venta y ventas ABIERTAS de la sede de la sesión. */
	@GetMapping("/ventas/por-cobrar")
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<PorCobrarDTO> listarPorCobrar() {
		return ResponseEntity.ok(servicioVenta.listarPorCobrar(usuarioActual.idSede()));
	}

	/** [It. 2] CUS25: productos de venta con precio y stock de la sede de la sesión (RF35). */
	@GetMapping("/ventas/productos")
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<List<ProductoStockDTO>> buscarProductos(@RequestParam(required = false) String nombre, @RequestParam(required = false) String marca,
			@RequestParam(required = false) Long idCategoria) {
		return ResponseEntity.ok(servicioVenta.buscarProductos(nombre, marca, idCategoria));
	}

	/** [It. 2] CUS26: 201 con la venta CERRADA; 202 con la venta ABIERTA si el cobro digital quedó PENDIENTE (RN52). */
	@PostMapping("/ventas/productos")
	@PreAuthorize("hasAuthority('VENTAS:CREAR')")
	public ResponseEntity<VentaDTO> venderProductos(@Valid @RequestBody VentaProductosDTO oVentaProductosDTO) {
		VentaDTO creada = servicioVenta.venderProductos(oVentaProductosDTO);
		URI ubicacion = URI.create("/api/v1/ventas/" + creada.idVenta());
		if (!creada.cobrosPendientes().isEmpty()) {
			return ResponseEntity.accepted().location(ubicacion).body(creada);
		}
		return ResponseEntity.created(ubicacion).body(creada);
	}

	/** [It. 2] CUS18: la venta con el estado actual de sus cobros digitales (RN52). */
	@GetMapping("/ventas/{idVenta}")
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<VentaDTO> obtenerVenta(@PathVariable Long idVenta) {
		return ResponseEntity.ok(servicioVenta.obtenerVenta(idVenta));
	}

	@PostMapping("/ventas")
	@PreAuthorize("hasAuthority('VENTAS:CREAR')")
	public ResponseEntity<VentaDTO> crearDesdeAtencion(@Valid @RequestBody CrearVentaDTO oCrearVentaDTO) {
		VentaDTO creada = servicioVenta.crearDesdeAtencion(oCrearVentaDTO.idAtencion());
		return ResponseEntity.created(URI.create("/api/v1/ventas/" + creada.idVenta())).body(creada);
	}

	@PatchMapping("/ventas/{idVenta}/descuentos")
	@PreAuthorize("hasAuthority('VENTAS:CREAR')")
	public ResponseEntity<VentaDTO> aplicarDescuentos(@PathVariable Long idVenta, @Valid @RequestBody DescuentosDTO oDescuentosDTO) {
		return ResponseEntity.ok(servicioVenta.aplicarDescuentos(idVenta, oDescuentosDTO));
	}

	/** 200; 202 si algún cobro quedó PENDIENTE en la pasarela (U2 §5). */
	@PostMapping("/ventas/{idVenta}/pagos")
	@PreAuthorize("hasAuthority('VENTAS:CREAR')")
	public ResponseEntity<VentaDTO> registrarPagos(@PathVariable Long idVenta, @Valid @RequestBody PagosDTO oPagosDTO) {
		VentaDTO oVentaDTO = servicioVenta.registrarPagos(idVenta, oPagosDTO);
		return ResponseEntity.status(oVentaDTO.cobrosPendientes().isEmpty() ? HttpStatus.OK : HttpStatus.ACCEPTED).body(oVentaDTO);
	}

	/** 200 con los datos; 202 si no se encontró o la consulta falló, para seguir a mano (RN45). */
	@GetMapping("/ventas/consulta-ruc/{ruc}")
	@PreAuthorize("hasAuthority('VENTAS:VER')")
	public ResponseEntity<ResultadoConsulta> consultarRuc(@PathVariable String ruc) {
		ResultadoConsulta oResultadoConsulta = servicioVenta.consultarRuc(ruc);
		HttpStatus estado = ENCONTRADO.equals(oResultadoConsulta.resultado()) ? HttpStatus.OK : HttpStatus.ACCEPTED;
		return ResponseEntity.status(estado).body(oResultadoConsulta);
	}

	@PostMapping("/ventas/{idVenta}/cierre")
	@PreAuthorize("hasAuthority('VENTAS:CREAR')")
	public ResponseEntity<VentaDTO> cerrar(@PathVariable Long idVenta, @Valid @RequestBody CierreVentaDTO oCierreVentaDTO) {
		return ResponseEntity.ok(servicioVenta.cerrar(idVenta, oCierreVentaDTO));
	}

	/** 200 y no 201: la proforma no se guarda (RN09). */
	@PostMapping("/proformas")
	@PreAuthorize("hasAuthority('VENTAS:CREAR')")
	public ResponseEntity<ProformaDTO> armarProforma(@Valid @RequestBody ProformaDTO oProformaDTO) {
		return ResponseEntity.ok(servicioVenta.armarProforma(oProformaDTO));
	}

}
