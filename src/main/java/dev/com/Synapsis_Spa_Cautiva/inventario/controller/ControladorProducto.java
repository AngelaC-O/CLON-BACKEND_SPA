package dev.com.Synapsis_Spa_Cautiva.inventario.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.UsuarioActual;
import dev.com.Synapsis_Spa_Cautiva.inventario.api.ConsumoPendienteDTO;
import dev.com.Synapsis_Spa_Cautiva.inventario.service.ServicioProducto;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
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

/** [It. 2] CUS66, CUS32, CUS33, CUS34 y CUS35. La sede es la de la sesión. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ControladorProducto {

	private final ServicioProducto servicioProducto;

	private final UsuarioActual usuarioActual;

	@GetMapping("/categorias-producto")
	@PreAuthorize("hasAuthority('PRODUCTOS:VER')")
	public ResponseEntity<List<CategoriaProductoDTO>> listarCategorias() {
		return ResponseEntity.ok(servicioProducto.listarCategorias());
	}

	@PostMapping("/categorias-producto")
	@PreAuthorize("hasAuthority('PRODUCTOS:CREAR')")
	public ResponseEntity<CategoriaProductoDTO> registrarCategoria(@RequestBody CategoriaProductoDTO oCategoriaDTO) {
		CategoriaProductoDTO creada = servicioProducto.registrarCategoria(oCategoriaDTO);
		return ResponseEntity.created(URI.create("/api/v1/categorias-producto/" + creada.idCategoria())).body(creada);
	}

	@GetMapping("/productos")
	@PreAuthorize("hasAuthority('PRODUCTOS:VER')")
	public ResponseEntity<List<ProductoDTO>> listarProductos(@RequestParam(required = false) String nombre,
			@RequestParam(required = false) Long idCategoria) {
		return ResponseEntity.ok(servicioProducto.listarProductos(nombre, idCategoria));
	}

	@PostMapping("/productos")
	@PreAuthorize("hasAuthority('PRODUCTOS:CREAR')")
	public ResponseEntity<ProductoDTO> registrar(@RequestBody ProductoDTO oProductoDTO) {
		ProductoDTO creado = servicioProducto.registrar(oProductoDTO);
		return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.idProducto())).body(creado);
	}

	@PostMapping("/productos/{idProducto}/salidas")
	@PreAuthorize("hasAuthority('KARDEX:CREAR')")
	public ResponseEntity<MovimientoDTO> registrarSalida(@PathVariable Long idProducto, @RequestBody MovimientoDTO oMovimientoDTO) {
		return ResponseEntity.ok(servicioProducto.registrarSalida(idProducto, oMovimientoDTO));
	}

	@PostMapping("/productos/{idProducto}/ajustes")
	@PreAuthorize("hasAuthority('KARDEX:CREAR')")
	public ResponseEntity<MovimientoDTO> registrarAjuste(@PathVariable Long idProducto, @RequestBody AjusteDTO oAjusteDTO) {
		return ResponseEntity.ok(servicioProducto.registrarAjuste(idProducto, oAjusteDTO));
	}

	/** Sin idSede, la de la sesión; sin fechas, los últimos 30 días. */
	@GetMapping("/productos/{idProducto}/kardex")
	@PreAuthorize("hasAuthority('KARDEX:VER')")
	public ResponseEntity<List<MovimientoKardexDTO>> consultarKardex(@PathVariable Long idProducto, @RequestParam(required = false) Long idSede,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
		return ResponseEntity.ok(servicioProducto.consultarKardex(idProducto, idSede, fechaInicio, fechaFin));
	}

	@GetMapping("/consumos-pendientes")
	@PreAuthorize("hasAuthority('KARDEX:VER')")
	public ResponseEntity<List<ConsumoPendienteDTO>> listarConsumosPendientes() {
		return ResponseEntity.ok(servicioProducto.listarConsumosPendientes(usuarioActual.idSede()));
	}

	@PostMapping("/consumos-pendientes/{idConsumo}/regularizacion")
	@PreAuthorize("hasAuthority('KARDEX:CREAR')")
	public ResponseEntity<ConsumoPendienteDTO> regularizar(@PathVariable Long idConsumo) {
		return ResponseEntity.ok(servicioProducto.regularizar(idConsumo));
	}

	@PatchMapping("/consumos-pendientes/{idConsumo}/descarte")
	@PreAuthorize("hasAuthority('KARDEX:CREAR')")
	public ResponseEntity<ConsumoPendienteDTO> descartar(@PathVariable Long idConsumo, @RequestBody DescarteDTO oDescarteDTO) {
		return ResponseEntity.ok(servicioProducto.descartar(idConsumo, oDescarteDTO.observacion()));
	}

}
