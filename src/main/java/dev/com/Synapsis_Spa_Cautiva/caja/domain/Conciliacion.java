package dev.com.Synapsis_Spa_Cautiva.caja.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.pagos.api.TransaccionLiquidadaDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobanteSunatDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentaResumenDTO;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Conciliación mensual de toda la empresa, un solo RUC (CUS47, RF75, RN31): las ventas del sistema frente a los comprobantes
 * de SUNAT y a las transacciones de la pasarela con su liquidación. Solo la Administradora la escribe (TRG_ADM_CONCILIACION).
 * Una por mes (UQ_CONCILIACION_ANIO_MES); se cierra cuando no queda ninguna diferencia SIN_JUSTIFICAR.
 */
@Entity
@Table(name = "CONCILIACION")
@Getter
public class Conciliacion {

	private static final String VENTA_ANULADA = "ANULADA";

	private static final String ACEPTADO = "ACEPTADO";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_conciliacion")
	private Long idConciliacion;

	@Column(name = "anio", nullable = false)
	private Integer anio;

	@Column(name = "mes", nullable = false)
	private Integer mes;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "fecha_cierre")
	private LocalDateTime fechaCierre;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoConciliacion estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "conciliacion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ConciliacionItem> items = new ArrayList<>();

	protected Conciliacion() {
	}

	/** CK_CONCILIACION_ANIO y CK_CONCILIACION_MES, para responder antes de llegar a Oracle. */
	public Conciliacion(Integer anio, Integer mes, Long idUsuario) {
		if (anio == null || anio < 2026 || anio > 2100 || mes == null || mes < 1 || mes > 12) {
			throw new ReglaNegocioException("RF75", "Indique un mes válido desde 2026.");
		}
		this.anio = anio;
		this.mes = mes;
		this.idUsuario = idUsuario;
		this.estado = EstadoConciliacion.ABIERTA;
	}

	/**
	 * RF75, RF76, RN31, RN33. Un ítem por venta del mes: coincide si SUNAT aceptó su comprobante y sus cobros digitales están
	 * abonados por la pasarela (el efectivo se respalda en caja). Además, un ítem SIN_JUSTIFICAR por cada comprobante sin venta
	 * vigente (salvo el de una venta anulada cuya nota de crédito SUNAT aceptó, que se excluye) y por cada transacción que no
	 * respalda una venta del mes: cobro sin venta ni cita, adelanto sin abono o devolución.
	 */
	public void generarItems(List<VentaResumenDTO> ventas, List<ComprobanteSunatDTO> comprobantes, List<TransaccionLiquidadaDTO> transacciones) {
		exigirAbierta();
		if (!items.isEmpty()) {
			throw new ReglaNegocioException("RF75", "La conciliación del mes ya tiene sus ítems.");
		}
		Map<Long, ComprobanteSunatDTO> comprobantePorVenta = comprobantes.stream()
			.filter(comprobante -> comprobante.idVenta() != null)
			.collect(Collectors.toMap(ComprobanteSunatDTO::idVenta, Function.identity(), (primero, otro) -> primero));
		Map<Long, List<TransaccionLiquidadaDTO>> cobrosPorVenta = transacciones.stream()
			.filter(transaccion -> transaccion.esCobro() && transaccion.idVenta() != null)
			.collect(Collectors.groupingBy(TransaccionLiquidadaDTO::idVenta));
		Set<Long> ventasDelMes = new HashSet<>();

		for (VentaResumenDTO venta : ventas) {
			ventasDelMes.add(venta.idVenta());
			ComprobanteSunatDTO comprobante = comprobantePorVenta.get(venta.idVenta());
			List<TransaccionLiquidadaDTO> cobros = cobrosPorVenta.getOrDefault(venta.idVenta(), List.of());
			Long cobroSinAbono = cobros.stream().filter(cobro -> !cobro.abonada()).map(TransaccionLiquidadaDTO::idTransaccion).findFirst()
				.orElse(null);
			String referencia = comprobante != null ? comprobante.referencia() : referencia(venta.serie(), venta.numero());
			items.add(new ConciliacionItem(this, venta.idVenta(), referencia, cobroSinAbono, comprobante != null && comprobante.aceptado(),
					cobroSinAbono == null));
		}
		for (ComprobanteSunatDTO comprobante : comprobantes) {
			boolean anuladaConNota = VENTA_ANULADA.equals(comprobante.estadoVenta()) && ACEPTADO.equals(comprobante.ncEstadoSunat());
			if (!ventasDelMes.contains(comprobante.idVenta()) && !anuladaConNota) {
				items.add(new ConciliacionItem(this, comprobante.idVenta(), comprobante.referencia(), null, comprobante.aceptado(), false));
			}
		}
		for (TransaccionLiquidadaDTO transaccion : transacciones) {
			boolean respaldaVentaDelMes = transaccion.esCobro() && ventasDelMes.contains(transaccion.idVenta());
			boolean adelantoAbonado = transaccion.esCobro() && transaccion.idVenta() == null && transaccion.idCita() != null
					&& transaccion.abonada();
			if (!respaldaVentaDelMes && !adelantoAbonado) {
				items.add(new ConciliacionItem(this, null, null, transaccion.idTransaccion(), false, transaccion.abonada()));
			}
		}
	}

	/** RF75, RN31: la diferencia queda JUSTIFICADA (con su explicación) o EXCLUIDA. */
	public void justificarItem(Long idItem, EstadoConciliacionItem estado, String justificacion) {
		exigirAbierta();
		ConciliacionItem item = items.stream()
			.filter(candidato -> Objects.equals(candidato.getIdItem(), idItem))
			.findFirst()
			.orElseThrow(() -> new ReglaNegocioException("RF75", "El ítem " + idItem + " no es de esta conciliación."));
		switch (estado) {
			case JUSTIFICADA -> item.justificar(justificacion);
			case EXCLUIDA -> item.excluir(justificacion);
			default -> throw new ReglaNegocioException("RF75", "Una diferencia se marca JUSTIFICADA o EXCLUIDA.");
		}
	}

	/** RF75: se cierra sin diferencias SIN_JUSTIFICAR y ya no cambia (CK_CONCILIACION_CIERRE). */
	public void cerrar(LocalDateTime fechaCierre) {
		exigirAbierta();
		long pendientes = items.stream().filter(ConciliacionItem::sinJustificar).count();
		if (pendientes > 0) {
			throw new ReglaNegocioException("RF75", "Quedan " + pendientes + " diferencia(s) sin justificar.");
		}
		this.fechaCierre = fechaCierre;
		this.estado = EstadoConciliacion.CERRADA;
	}

	private void exigirAbierta() {
		if (estado != EstadoConciliacion.ABIERTA) {
			throw new ReglaNegocioException("RF75", "La conciliación está " + estado + " y no cambia.");
		}
	}

	private static String referencia(String serie, String numero) {
		return serie == null ? null : serie + "-" + numero;
	}

	public List<ConciliacionItem> getItems() {
		return Collections.unmodifiableList(items);
	}

}
