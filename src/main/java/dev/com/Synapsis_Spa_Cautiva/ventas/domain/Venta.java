package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

import lombok.AccessLevel;
import lombok.Getter;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "VENTA")
@DynamicUpdate
@Getter
public class Venta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_venta")
	private Long idVenta;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Column(name = "id_caja", nullable = false)
	private Long idCaja;

	@Column(name = "id_cliente")
	private Long idCliente;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "id_usuario_autoriza")
	private Long idUsuarioAutoriza;

	@Column(name = "fecha_hora", updatable = false, nullable = false)
	private LocalDateTime fechaHora;

	@Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "igv", nullable = false, precision = 10, scale = 2)
	private BigDecimal igv;

	@Column(name = "descuento", nullable = false, precision = 10, scale = 2)
	private BigDecimal descuento;

	@Column(name = "adelanto_aplicado", nullable = false, precision = 10, scale = 2)
	private BigDecimal adelantoAplicado;

	@Column(name = "saldo_favor_aplicado", nullable = false, precision = 10, scale = 2)
	private BigDecimal saldoFavorAplicado;

	@Column(name = "total", nullable = false, precision = 10, scale = 2)
	private BigDecimal total;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoVenta estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VentaDetalle> detalles = new ArrayList<>();

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VentaPago> pagos = new ArrayList<>();

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comprobante> comprobantes = new ArrayList<>();

	protected Venta() {
	}

	public Venta(Long idSede, Long idCaja, Long idCliente, Long idUsuario) {
		this.idSede = idSede;
		this.idCaja = idCaja;
		this.idCliente = idCliente;
		this.idUsuario = idUsuario;
		this.fechaHora = LocalDateTime.now();
		this.subtotal = BigDecimal.ZERO;
		this.igv = BigDecimal.ZERO;
		this.descuento = BigDecimal.ZERO;
		this.adelantoAplicado = BigDecimal.ZERO;
		this.saldoFavorAplicado = BigDecimal.ZERO;
		this.total = BigDecimal.ZERO;
		this.estado = EstadoVenta.ABIERTA;
	}

	public void agregarServicio(Long idAtencion, BigDecimal precioUnitario, Long idPersonal) {
		exigirAbierta();
		detalles.add(new VentaDetalle(this, idAtencion, precioUnitario, idPersonal));
		recalcularTotales();
	}

	/** El descuento de cada línea lo calcula TRG_VENTA_DETALLE_REGLAS según la promoción (RN25). */
	public void aplicarPromocion(Long idPromocion) {
		exigirAbierta();
		detalles.stream()
			.filter(detalle -> detalle.getTipoItem() == TipoItemVentaDetalle.SERVICIO)
			.forEach(detalle -> detalle.aplicarPromocion(idPromocion));
	}

	public void aplicarAdelanto(BigDecimal adelantoMonto) {
		exigirAbierta();
		this.adelantoAplicado = adelantoMonto;
		recalcularTotales();
	}

	public void aplicarSaldoFavor(BigDecimal saldoFavorAplicado) {
		exigirAbierta();
		this.saldoFavorAplicado = saldoFavorAplicado;
		recalcularTotales();
	}

	public void registrarPago(Long idMedioPago, BigDecimal monto, Long idTransaccion) {
		exigirAbierta();
		if (monto == null || monto.signum() <= 0) {
			throw new ReglaNegocioException("RN11", "El monto de un pago debe ser mayor que 0.");
		}
		pagos.add(new VentaPago(this, idMedioPago, monto, idTransaccion));
	}

	/** Serie, número y fecha los asigna Oracle al insertar (RN47). */
	public void generarComprobante(TipoComprobante tipo, DocumentoComprador oDocumentoComprador) {
		exigirAbierta();
		if (tipo != TipoComprobante.BOLETA && tipo != TipoComprobante.FACTURA) {
			throw new ReglaNegocioException("RN10", "Una venta abierta genera boleta o factura.");
		}
		if (tieneComprobante()) {
			throw new ReglaNegocioException("RN10", "La venta ya tiene su comprobante.");
		}
		if (tipo == TipoComprobante.FACTURA && (oDocumentoComprador == null || !oDocumentoComprador.estaCompleto())) {
			throw new ReglaNegocioException("RN08", "La factura exige el tipo, el número y el nombre del comprador.");
		}
		comprobantes.add(new Comprobante(this, tipo, oDocumentoComprador));
	}

	/** RN10: sin comprobante no se cierra. RN11: los pagos suman exactamente el total. */
	public void cerrar() {
		exigirAbierta();
		if (!tieneComprobante()) {
			throw new ReglaNegocioException("RN10", "Genere la boleta o la factura antes de cerrar la venta.");
		}
		recalcularTotales();
		BigDecimal pagado = pagos.stream().map(VentaPago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (pagado.compareTo(total) != 0) {
			throw new ReglaNegocioException("RN11", "Los pagos suman S/ " + pagado + " y el total es S/ " + total + ".");
		}
		this.estado = EstadoVenta.CERRADA;
	}

	private boolean tieneComprobante() {
		return comprobantes.stream()
			.anyMatch(comprobante -> comprobante.getTipo() == TipoComprobante.BOLETA || comprobante.getTipo() == TipoComprobante.FACTURA);
	}

	private void exigirAbierta() {
		if (estado != EstadoVenta.ABIERTA) {
			throw new ReglaNegocioException("RN12", "La venta está " + estado + " y no se modifica.");
		}
	}

	/** Mantiene CK_VENTA_TOTAL en cada cambio; el IGV se resta del importe porque los precios ya lo incluyen (RN06). */
	private void recalcularTotales() {
		BigDecimal importe = detalles.stream().map(VentaDetalle::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add).subtract(descuento);
		BigDecimal aPagar = importe.subtract(adelantoAplicado).subtract(saldoFavorAplicado);
		if (aPagar.signum() < 0) {
			throw new ReglaNegocioException("RF25", "El adelanto y el saldo a favor superan el importe de la venta.");
		}
		this.subtotal = importe.subtract(igv);
		this.total = aPagar;
	}

	public List<VentaDetalle> getDetalles() {
		return Collections.unmodifiableList(detalles);
	}

	public List<VentaPago> getPagos() {
		return Collections.unmodifiableList(pagos);
	}

	public List<Comprobante> getComprobantes() {
		return Collections.unmodifiableList(comprobantes);
	}

}
