package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.net.URI;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.service.ServicioCliente;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.ResultadoConsulta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ControladorCliente {

	private static final String ENCONTRADO = "ENCONTRADO";

	private final ServicioCliente servicioCliente;

	@GetMapping
	@PreAuthorize("hasAuthority('CLIENTAS:VER')")
	public ResponseEntity<List<ClienteDTO>> buscar(@RequestParam(required = false) String nombres, @RequestParam(required = false) String telefono) {
		return ResponseEntity.ok(servicioCliente.buscar(nombres, telefono));
	}

	/** 200 con los datos; 202 si no se encontró o la consulta falló, para seguir a mano (RN45). */
	@GetMapping("/consulta-documento")
	@PreAuthorize("hasAuthority('CLIENTAS:VER')")
	public ResponseEntity<ResultadoConsulta> consultarDocumento(@RequestParam Long idTipoDocumento, @RequestParam String numeroDocumento) {
		ResultadoConsulta oResultadoConsulta = servicioCliente.consultarDocumento(idTipoDocumento, numeroDocumento);
		HttpStatus estado = ENCONTRADO.equals(oResultadoConsulta.resultado()) ? HttpStatus.OK : HttpStatus.ACCEPTED;
		return ResponseEntity.status(estado).body(oResultadoConsulta);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('CLIENTAS:CREAR')")
	public ResponseEntity<ClienteDTO> registrar(@Valid @RequestBody ClienteDTO oClienteDTO) {
		ClienteDTO creado = servicioCliente.registrar(oClienteDTO);
		return ResponseEntity.created(URI.create("/api/v1/clientes/" + creado.idCliente())).body(creado);
	}

	/** [It. 2] CUS03: la ficha de la clienta (RF02). */
	@GetMapping("/{idCliente}")
	@PreAuthorize("hasAuthority('CLIENTAS:VER')")
	public ResponseEntity<FichaClienteDTO> obtenerFicha(@PathVariable Long idCliente) {
		return ResponseEntity.ok(servicioCliente.obtenerFicha(idCliente));
	}

	/** [It. 2] CUS04: los datos comunes van a PERSONA y los propios a CLIENTE (RF02, RN27, RN45). */
	@PutMapping("/{idCliente}")
	@PreAuthorize("hasAuthority('CLIENTAS:EDITAR')")
	public ResponseEntity<ClienteDTO> actualizar(@PathVariable Long idCliente, @Valid @RequestBody ClienteDTO oClienteDTO) {
		return ResponseEntity.ok(servicioCliente.actualizar(idCliente, oClienteDTO));
	}

}
