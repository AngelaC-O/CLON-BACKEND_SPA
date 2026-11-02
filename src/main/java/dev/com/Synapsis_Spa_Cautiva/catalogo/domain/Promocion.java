package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "PROMOCION")
@Getter
public class Promocion {

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

	protected Promocion() {
	}

}
