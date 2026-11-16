package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoClientePaquete;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ProductoCompradoDTO;

/**
 * [It. 2 · CUS03, RF02] La ficha de la clienta: datos de contacto (PERSONA por PersonaApi) y de CLIENTE, saldo a favor (V_SALDO_FAVOR_CLIENTE,
 * RN05), historial de atenciones con sus recomendaciones (CUS12), citas por atender, paquetes con sesiones por usar (V_SESIONES_PAQUETE, RN36)
 * y productos comprados (HistorialComprasProvider, que implementa ventas).
 */
public record FichaClienteDTO(ClienteDTO cliente, BigDecimal saldoFavor, List<AtencionFicha> atenciones, List<CitaDTO> citasPendientes,
		List<PaqueteFicha> paquetes, List<ProductoCompradoDTO> productosComprados) {

	public record AtencionFicha(Long idAtencion, Long idCita, Long idPersonal, Long idRecurso, Long idVariante, Long idClientePaquete,
			LocalDateTime horaInicio, LocalDateTime horaFin, String observaciones, EstadoAtencion estado, List<RecomendacionDTO> recomendaciones,
			String nombreServicio, String nombreEspecialista, String nombreRecurso) {

		/** [It. 2.1 · nombres] Duda 9: el historial muestra servicio, especialista y recurso; las recomendaciones, el nombre del producto. */
		public AtencionFicha conNombres(List<RecomendacionDTO> recomendaciones, String nombreServicio, String nombreEspecialista, String nombreRecurso) {
			return new AtencionFicha(idAtencion, idCita, idPersonal, idRecurso, idVariante, idClientePaquete, horaInicio, horaFin, observaciones, estado,
					recomendaciones, nombreServicio, nombreEspecialista, nombreRecurso);
		}

	}

	/** Las sesiones usadas se cuentan en ATENCION.id_cliente_paquete, no se guardan (RN36, RN37). */
	public record PaqueteFicha(Long idClientePaquete, Long idPaquete, Integer sesionesTotales, Integer sesionesUsadas, Integer sesionesRestantes,
			LocalDate fechaCompra, LocalDate fechaVencimiento, EstadoClientePaquete estado, String nombrePaquete) {
	}

}
