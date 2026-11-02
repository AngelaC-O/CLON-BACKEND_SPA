package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "INVENTARIO_SEDE")
@Getter
public class InventarioSede {

	@EmbeddedId
	private InventarioSedeId id = new InventarioSedeId();

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("idProducto")
	@JoinColumn(name = "id_producto")
	private Producto producto;

	@Column(name = "stock_actual", nullable = false)
	private Integer stockActual;

	@Column(name = "stock_minimo", nullable = false)
	private Integer stockMinimo;

	@Column(name = "ubicacion", length = 60)
	private String ubicacion;

	protected InventarioSede() {
	}

	public Long getIdProducto() {
		return id.getIdProducto();
	}

	public Long getIdSede() {
		return id.getIdSede();
	}

}
