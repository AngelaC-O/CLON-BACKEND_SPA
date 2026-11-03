package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

/** PK compuesta de SERVICIO_INSUMO. */
@Embeddable
@Getter
public class ServicioInsumoId implements Serializable {

	@Column(name = "id_variante")
	private Long idVariante;

	@Column(name = "id_producto")
	private Long idProducto;

	protected ServicioInsumoId() {
	}

	ServicioInsumoId(Long idProducto) {
		this.idProducto = idProducto;
	}

	@Override
	public boolean equals(Object otro) {
		return otro instanceof ServicioInsumoId id && Objects.equals(idVariante, id.idVariante) && Objects.equals(idProducto, id.idProducto);
	}

	@Override
	public int hashCode() {
		return Objects.hash(idVariante, idProducto);
	}

}
