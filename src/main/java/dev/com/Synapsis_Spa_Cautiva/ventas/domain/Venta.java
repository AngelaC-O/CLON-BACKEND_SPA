package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.http.HttpStatus;

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

	/** TRG_VENTA_REGLAS pone el usuario de PKG_SESION y la fecha de Oracle al insertar. */
	@Generated(event = EventType.INSERT, writable = true)
	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	/** Obsoleta desde v5.1 (CK_VENTA_AUTORIZA_OBSOLETA, -20100): solo se lee y no se usa; la venta tardía reabre la caja (RN30). */
	@Column(name = "id_usuario_autoriza", insertable = false, updatable = false)
	private Long idUsuarioAutoriza;

	@Generated(event = EventType.INSERT, writable = true)
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

	/**
	 * RF36, RF38: línea de producto; TRG_VENTA_DETALLE_REGLAS pone el precio del catálogo y rechaza un insumo o la falta de
	 * stock (RN13, RN15). idPersonal es la especialista que lo recomendó; los productos no comisionan (RN38).
	 */
	public void agregarProducto(Long idProducto, Integer cantidad, BigDecimal precioUnitario, Long idPersonal) {
		exigirAbierta();
		if (cantidad == null || cantidad <= 0) {
			throw new ReglaNegocioException("RF36", "La cantidad de un producto es mayor que 0.");
		}
		if (precioUnitario == null || precioUnitario.signum() < 0) {
			throw new ReglaNegocioException("RN14", "El precio del producto es mayor o igual a 0.");
		}
		detalles.add(VentaDetalle.producto(this, idProducto, cantidad, precioUnitario, idPersonal));
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

	/**
	 * RF32, RF34, RN12, RN33: la venta CERRADA pasa a ANULADA con su nota de crédito, o sin ella si SUNAT había rechazado el
	 * comprobante (lo repite PKG_VENTA.validar_anulacion, -20058). Los efectos los hace Oracle (TRG_VENTA_EFECTOS →
	 * PKG_VENTA.al_anular): devuelve la atención, anula el paquete, abona el saldo y reingresa los productos.
	 * idUsuarioAutoriza es la Administradora que aprobó la solicitud; no se guarda en VENTA (columna obsoleta, v5.1).
	 */
	public void anular(Long idUsuarioAutoriza) {
		if (estado != EstadoVenta.CERRADA) {
			throw new ReglaNegocioException("RN12", "Solo se anula una venta CERRADA; esta está " + estado + ".");
		}
		if (idUsuarioAutoriza == null) {
			throw new ReglaNegocioException("RN32", "La anulación la autoriza la Administradora.");
		}
		boolean conNota = comprobantes.stream().anyMatch(NotaCredito.class::isInstance);
		boolean rechazadoPorSunat = comprobanteDeVenta().map(comprobante -> comprobante.getEstadoSunat() == EstadoSunat.RECHAZADO)
			.orElse(false);
		if (!conNota && !rechazadoPorSunat) {
			throw new ReglaNegocioException("RN12", "Emita la nota de crédito antes de anular la venta.");
		}
		this.estado = EstadoVenta.ANULADA;
	}

	/** RN12: una sola nota por venta, de la misma clase que su comprobante (NC_BOLETA anula BOLETA, UQ_COMPROBANTE_VENTA_CLASE). */
	void agregarNotaCredito(NotaCredito oNotaCredito) {
		if (estado != EstadoVenta.CERRADA) {
			throw new ReglaNegocioException("RN12", "Solo se emite la nota de crédito de una venta CERRADA; esta está " + estado + ".");
		}
		Comprobante comprobante = comprobanteDeVenta()
			.orElseThrow(() -> new ReglaNegocioException("RN12", "La venta no tiene boleta ni factura que anular."));
		if (!oNotaCredito.getTipo().name().equals("NC_" + comprobante.getTipo().name())) {
			throw new ReglaNegocioException("RN12", "La " + comprobante.getTipo() + " se anula con una NC_" + comprobante.getTipo() + ".");
		}
		if (comprobantes.stream().anyMatch(NotaCredito.class::isInstance)) {
			throw new ReglaNegocioException("RN12", "La venta ya tiene su nota de crédito.", HttpStatus.CONFLICT);
		}
		comprobantes.add(oNotaCredito);
	}

	private Optional<Comprobante> comprobanteDeVenta() {
		return comprobantes.stream()
			.filter(comprobante -> comprobante.getTipo() == TipoComprobante.BOLETA || comprobante.getTipo() == TipoComprobante.FACTURA)
			.findFirst();
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
