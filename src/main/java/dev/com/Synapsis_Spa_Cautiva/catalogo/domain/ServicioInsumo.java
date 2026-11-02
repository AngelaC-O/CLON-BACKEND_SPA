package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.math.BigDecimal;

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
@Table(name = "SERVICIO_INSUMO")
@Getter
public class ServicioInsumo {

	@EmbeddedId
	private ServicioInsumoId id = new ServicioInsumoId();

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("idVariante")
	@JoinColumn(name = "id_variante")
	private VarianteServicio variante;

	@Column(name = "cantidad_estimada", nullable = false, precision = 10, scale = 2)
	private BigDecimal cantidadEstimada;

	protected ServicioInsumo() {
	}

	ServicioInsumo(VarianteServicio variante, Long idProducto, BigDecimal cantidadEstimada) {
		this.variante = variante;
		this.id = new ServicioInsumoId(idProducto);
		this.cantidadEstimada = cantidadEstimada;
	}

	public Long getIdVariante() {
		return id.getIdVariante();
	}

	public Long getIdProducto() {
		return id.getIdProducto();
	}

}
