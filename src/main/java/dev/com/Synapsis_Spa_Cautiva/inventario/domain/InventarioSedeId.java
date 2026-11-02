package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/** PK compuesta de INVENTARIO_SEDE. */
@Embeddable
@Getter
public class InventarioSedeId implements Serializable {

	@Column(name = "id_producto")
	private Long idProducto;

	@Column(name = "id_sede")
	private Long idSede;

	protected InventarioSedeId() {
	}

	InventarioSedeId(Long idSede) {
		this.idSede = idSede;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof InventarioSedeId id && Objects.equals(idProducto, id.idProducto) && Objects.equals(idSede, id.idSede);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idProducto, idSede);
	}

}
