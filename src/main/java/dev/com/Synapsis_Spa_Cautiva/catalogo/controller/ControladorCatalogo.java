package dev.com.Synapsis_Spa_Cautiva.catalogo.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoServicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.service.ServicioCatalogo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ControladorCatalogo {

	private final ServicioCatalogo servicioCatalogo;

	@GetMapping("/areas")
	@PreAuthorize("hasAuthority('CATALOGO:VER')")
	public ResponseEntity<List<AreaDTO>> listarAreas() {
		return ResponseEntity.ok(servicioCatalogo.listarAreas());
	}

	@PostMapping("/areas")
	@PreAuthorize("hasAuthority('CATALOGO:CREAR')")
	public ResponseEntity<AreaDTO> registrarArea(@Valid @RequestBody AreaDTO oAreaDTO) {
		AreaDTO creada = servicioCatalogo.registrarArea(oAreaDTO.linea(), oAreaDTO.nombreArea());
		return ResponseEntity.created(URI.create("/api/v1/areas/" + creada.idArea())).body(creada);
	}

	/** [It. 2] CUS58: servicios con variantes y precios (RF83); filtros opcionales. */
	@GetMapping("/servicios")
	@PreAuthorize("hasAuthority('CATALOGO:VER')")
	public ResponseEntity<List<ServicioDTO>> listarServicios(@RequestParam(required = false) Long idArea,
			@RequestParam(required = false) EstadoServicio estado) {
		return ResponseEntity.ok(servicioCatalogo.listarServicios(idArea, estado));
	}

	@PostMapping("/servicios")
	@PreAuthorize("hasAuthority('CATALOGO:CREAR')")
	public ResponseEntity<ServicioDTO> registrarServicio(@Valid @RequestBody ServicioDTO oServicioDTO) {
		ServicioDTO creado = servicioCatalogo.registrarServicio(oServicioDTO);
		return ResponseEntity.created(URI.create("/api/v1/servicios/" + creado.idServicio())).body(creado);
	}

	/** [It. 2] CUS58: datos e insumos del servicio; desactivar es estado INACTIVO (RF83). */
	@PutMapping("/servicios/{idServicio}")
	@PreAuthorize("hasAuthority('CATALOGO:EDITAR')")
	public ResponseEntity<ServicioDTO> actualizarServicio(@PathVariable Long idServicio, @Valid @RequestBody ServicioDTO oServicioDTO) {
		return ResponseEntity.ok(servicioCatalogo.actualizarServicio(idServicio, oServicioDTO));
	}

	/** [It. 2] CUS58: solo la Administradora cambia precios (RN25); lo audita TRG_AUD_VARIANTE_SERVICIO. */
	@PatchMapping("/servicios/{idServicio}/variantes/{idVariante}/precio")
	@PreAuthorize("hasAuthority('CATALOGO:EDITAR')")
	public ResponseEntity<ServicioDTO> cambiarPrecio(@PathVariable Long idServicio, @PathVariable Long idVariante, @RequestBody PrecioDTO oPrecioDTO) {
		return ResponseEntity.ok(servicioCatalogo.cambiarPrecio(idServicio, idVariante, oPrecioDTO.precioNuevo()));
	}

	/** [It. 2] CUS59: paquetes y membresías (RF84); sin tipo, todos. */
	@GetMapping("/paquetes")
	@PreAuthorize("hasAuthority('CATALOGO:VER')")
	public ResponseEntity<List<PaqueteDTO>> listarPaquetes(@RequestParam(required = false) TipoPaquete tipo) {
		return ResponseEntity.ok(servicioCatalogo.listarPaquetes(tipo));
	}

	@PostMapping("/paquetes")
	@PreAuthorize("hasAuthority('CATALOGO:CREAR')")
	public ResponseEntity<PaqueteDTO> registrarPaquete(@Valid @RequestBody PaqueteDTO oPaqueteDTO) {
		PaqueteDTO creado = servicioCatalogo.registrarPaquete(oPaqueteDTO);
		return ResponseEntity.created(URI.create("/api/v1/paquetes/" + creado.idPaquete())).body(creado);
	}

	/** [It. 2] CUS60: con vigentes=true, solo las vigentes hoy (RF84). */
	@GetMapping("/promociones")
	@PreAuthorize("hasAuthority('CATALOGO:VER')")
	public ResponseEntity<List<PromocionDTO>> listarPromociones(@RequestParam(defaultValue = "false") boolean vigentes) {
		return ResponseEntity.ok(servicioCatalogo.listarPromociones(vigentes));
	}

	@PostMapping("/promociones")
	@PreAuthorize("hasAuthority('CATALOGO:CREAR')")
	public ResponseEntity<PromocionDTO> registrarPromocion(@Valid @RequestBody PromocionDTO oPromocionDTO) {
		PromocionDTO creada = servicioCatalogo.registrarPromocion(oPromocionDTO);
		return ResponseEntity.created(URI.create("/api/v1/promociones/" + creada.idPromocion())).body(creada);
	}

}
