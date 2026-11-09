package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * Promoción o descuento con vigencia, solo para servicios (CUS60, RF84). El descuento de cada línea lo calcula
 * TRG_VENTA_DETALLE_REGLAS (RN25). Solo la Administradora la crea (TRG_ADM_PROMOCION).
 */
@Entity
@Table(name = "PROMOCION")
@Getter
public class Promocion {

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_promocion")
	private Long idPromocion;

	@Column(name = "nombre", nullable = false, length = 80)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_descuento", nullable = false, length = 10)
	private TipoDescuentoPromocion tipoDescuento;

	@Column(name = "valor", nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	@Column(name = "fecha_fin", nullable = false)
	private LocalDate fechaFin;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoPromocion estado;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "promocion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PromocionServicio> servicios = new ArrayList<>();

	protected Promocion() {
	}

	/** CK_PROMOCION_VALOR y CK_PROMOCION_VIGENCIA, para responder antes de llegar a Oracle. */
	public Promocion(String nombre, TipoDescuentoPromocion tipoDescuento, BigDecimal valor, LocalDate fechaInicio, LocalDate fechaFin,
			Long idUsuario) {
		if (valor == null || valor.signum() <= 0 || (tipoDescuento == TipoDescuentoPromocion.PORCENTAJE && valor.compareTo(CIEN) > 0)) {
			throw new ReglaNegocioException("RF84", "El descuento es mayor que 0 y, si es un porcentaje, hasta 100.");
		}
		if (fechaInicio == null || fechaFin == null || fechaFin.isBefore(fechaInicio)) {
			throw new ReglaNegocioException("RF84", "La vigencia termina el mismo día o después de que empieza.");
		}
		this.nombre = nombre;
		this.tipoDescuento = tipoDescuento;
		this.valor = valor;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.idUsuario = idUsuario;
		this.estado = EstadoPromocion.ACTIVA;
	}

	/** RF84: un servicio se agrega una sola vez (PK_PROMOCION_SERVICIO). */
	public void aplicarAServicio(Long idServicio) {
		boolean yaAplica = servicios.stream().anyMatch(servicio -> servicio.getIdServicio().equals(idServicio));
		if (!yaAplica) {
			servicios.add(new PromocionServicio(this, idServicio));
		}
	}

	public List<PromocionServicio> getServicios() {
		return Collections.unmodifiableList(servicios);
	}

}
