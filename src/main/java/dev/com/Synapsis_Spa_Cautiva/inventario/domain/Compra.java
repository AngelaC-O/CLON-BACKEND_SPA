package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

/**
 * Solo estructura en la Iteración 1: sus CUS son de otra iteración.
 */
@Entity
@Table(name = "COMPRA")
@Getter
public class Compra {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_compra")
	private Long idCompra;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Column(name = "id_proveedor", nullable = false)
	private Long idProveedor;

	@Column(name = "id_requerimiento")
	private Long idRequerimiento;

	@Column(name = "documento", nullable = false, length = 30)
	private String documento;

	@Column(name = "fecha", nullable = false)
	private LocalDate fecha;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "total", nullable = false, precision = 10, scale = 2)
	private BigDecimal total;

	protected Compra() {
	}

}
