package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.SiNoConverter;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "PRODUCTO")
@Getter
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_producto")
	private Long idProducto;

	@Column(name = "id_categoria", nullable = false)
	private Long idCategoria;

	@Column(name = "codigo", nullable = false, length = 15)
	private String codigo;

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@Column(name = "marca", length = 60)
	private String marca;

	@Column(name = "unidad", nullable = false, length = 15)
	private String unidad;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_uso", nullable = false, length = 10)
	private TipoUsoProducto tipoUso;

	@Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
	private BigDecimal precioVenta;

	@Column(name = "costo", nullable = false, precision = 10, scale = 2)
	private BigDecimal costo;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "es_premium", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean esPremium;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoProducto estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<InventarioSede> inventarios = new ArrayList<>();

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MovimientoKardex> movimientos = new ArrayList<>();

	protected Producto() {
	}

	public List<InventarioSede> getInventarios() {
		return Collections.unmodifiableList(inventarios);
	}

	public List<MovimientoKardex> getMovimientos() {
		return Collections.unmodifiableList(movimientos);
	}

}
