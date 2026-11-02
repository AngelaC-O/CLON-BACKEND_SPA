package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import java.math.BigDecimal;

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
@Table(name = "COMPRA_DETALLE")
@Getter
public class CompraDetalle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_detalle_compra")
	private Long idDetalleCompra;

	@Column(name = "id_compra", nullable = false)
	private Long idCompra;

	@Column(name = "id_producto", nullable = false)
	private Long idProducto;

	@Column(name = "cantidad_recibida", nullable = false)
	private Integer cantidadRecibida;

	@Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
	private BigDecimal costoUnitario;

	protected CompraDetalle() {
	}

}
