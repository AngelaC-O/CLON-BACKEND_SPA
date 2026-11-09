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

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * El stock lo pone en 0 TRG_INVENTARIO_SEDE_REGLAS al insertar y solo lo cambia el kardex (TRG_KARDEX_APLICAR):
 * Java nunca lo escribe (RN14, RN18).
 */
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

	@Generated(event = EventType.INSERT)
	@Column(name = "stock_actual", insertable = false, updatable = false)
	private Integer stockActual;

	@Column(name = "stock_minimo", nullable = false)
	private Integer stockMinimo;

	@Column(name = "ubicacion", length = 60)
	private String ubicacion;

	protected InventarioSede() {
	}

	InventarioSede(Producto producto, Long idSede, Integer stockMinimo, String ubicacion) {
		this.producto = producto;
		this.id = new InventarioSedeId(idSede);
		this.stockMinimo = stockMinimo;
		this.ubicacion = ubicacion;
	}

	void definirMinimo(Integer stockMinimo, String ubicacion) {
		this.stockMinimo = stockMinimo;
		this.ubicacion = ubicacion;
	}

	public Long getIdProducto() {
		return id.getIdProducto();
	}

	public Long getIdSede() {
		return id.getIdSede();
	}

}
