package dev.com.Synapsis_Spa_Cautiva.catalogo.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "VARIANTE_SERVICIO")
@Getter
public class VarianteServicio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_variante")
	private Long idVariante;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_servicio", nullable = false)
	private Servicio servicio;

	@Column(name = "nombre", nullable = false, length = 60)
	private String nombre;

	@Column(name = "precio", nullable = false, precision = 10, scale = 2)
	private BigDecimal precio;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoVarianteServicio estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "variante", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ServicioInsumo> insumos = new ArrayList<>();

	protected VarianteServicio() {
	}

	VarianteServicio(Servicio servicio, String nombre, BigDecimal precio) {
		this.servicio = servicio;
		this.nombre = nombre;
		this.precio = precio;
		this.estado = EstadoVarianteServicio.ACTIVO;
	}

	void definirInsumo(Long idProducto, BigDecimal cantidadEstimada) {
		insumos.add(new ServicioInsumo(this, idProducto, cantidadEstimada));
	}

	public Long getIdServicio() {
		return servicio.getIdServicio();
	}

	public List<ServicioInsumo> getInsumos() {
		return Collections.unmodifiableList(insumos);
	}

}
