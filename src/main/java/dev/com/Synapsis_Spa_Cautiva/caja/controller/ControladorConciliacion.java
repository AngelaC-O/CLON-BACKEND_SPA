package dev.com.Synapsis_Spa_Cautiva.caja.controller;

import java.net.URI;

import dev.com.Synapsis_Spa_Cautiva.caja.service.ServicioConciliacion;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.ResumenImportacionDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** [It. 2] CUS47: la conciliación mensual, solo de la Administradora (CONCILIACION:*). */
@RestController
@RequestMapping("/api/v1/conciliaciones")
@RequiredArgsConstructor
public class ControladorConciliacion {

	private final ServicioConciliacion servicioConciliacion;

	/** RF105: multipart con el reporte de liquidación de Izipay (CSV o Excel) en la parte «archivoLiquidacion». */
	@PostMapping(path = "/liquidacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('CONCILIACION:CREAR')")
	public ResponseEntity<ResumenImportacionDTO> importarLiquidacion(@RequestPart("archivoLiquidacion") MultipartFile archivoLiquidacion) {
		return ResponseEntity.ok(servicioConciliacion.importarLiquidacion(archivoLiquidacion));
	}

	/** RF75, RF76: la conciliación del mes con sus diferencias; una por mes (409). */
	@PostMapping
	@PreAuthorize("hasAuthority('CONCILIACION:CREAR')")
	public ResponseEntity<ConciliacionDTO> conciliar(@RequestBody ConciliacionDTO oConciliacionDTO) {
		ConciliacionDTO creada = servicioConciliacion.conciliar(oConciliacionDTO.anio(), oConciliacionDTO.mes());
		return ResponseEntity.created(URI.create("/api/v1/conciliaciones/" + creada.idConciliacion())).body(creada);
	}

	@PatchMapping("/{idConciliacion}/items/{idItem}")
	@PreAuthorize("hasAuthority('CONCILIACION:EDITAR')")
	public ResponseEntity<ConciliacionDTO> justificarItem(@PathVariable Long idConciliacion, @PathVariable Long idItem,
			@Valid @RequestBody JustificacionDTO oJustificacionDTO) {
		return ResponseEntity.ok(servicioConciliacion.justificarItem(idConciliacion, idItem, oJustificacionDTO.estado(), oJustificacionDTO.justificacion()));
	}

	@PatchMapping("/{idConciliacion}/cierre")
	@PreAuthorize("hasAuthority('CONCILIACION:EDITAR')")
	public ResponseEntity<ConciliacionDTO> cerrar(@PathVariable Long idConciliacion) {
		return ResponseEntity.ok(servicioConciliacion.cerrar(idConciliacion));
	}

}
