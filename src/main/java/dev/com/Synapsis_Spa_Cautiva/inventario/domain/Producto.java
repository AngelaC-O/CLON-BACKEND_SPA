package dev.com.Synapsis_Spa_Cautiva.inventario.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import jakarta.persistence.Transient;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.comun.api.SiNoConverter;

import lombok.AccessLevel;
import lombok.Getter;

import org.springframework.http.HttpStatus;

/**
 * Raíz del agregado de inventario: el producto, su stock por sede (INVENTARIO_SEDE) y su kardex (MOVIMIENTO_KARDEX).
 * Java solo inserta el movimiento: TRG_KARDEX_APLICAR fija usuario, fecha y saldo y mueve INVENTARIO_SEDE (RN14, RN18).
 */
@Entity
@Table(name = "PRODUCTO")
@Getter
public class Producto {

	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

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

	/**
	 * El último movimiento guardado por sede en esta transacción, con el id y el saldo que puso Oracle. Lo avisa
	 * MovimientoKardex al insertarse: save() de un producto existente hace merge y Hibernate guarda una copia del
	 * movimiento, no el objeto que se agregó. Así estaEnMinimo tampoco carga todo el kardex del producto.
	 */
	@Getter(AccessLevel.NONE)
	@Transient
	private final Map<Long, MovimientoKardex> guardadosPorSede = new HashMap<>();

	protected Producto() {
	}

	/** RF44, RF45, RN13: el precio de venta lo fija calcularPrecioVenta con el margen de PARAMETRO (RF87). */
	public Producto(String codigo, String nombre, String marca, Long idCategoria, String unidad, TipoUsoProducto tipoUso,
			BigDecimal costo) {
		if (costo == null || costo.signum() < 0) {
			throw new ReglaNegocioException("RN14", "El costo del producto es mayor o igual a 0.");
		}
		this.codigo = codigo;
		this.nombre = nombre;
		this.marca = marca;
		this.idCategoria = idCategoria;
		this.unidad = unidad;
		this.tipoUso = tipoUso;
		this.costo = costo;
		this.precioVenta = costo;
		this.esPremium = Boolean.FALSE;
		this.estado = EstadoProducto.ACTIVO;
	}

	/**
	 * RF87, RN14: precio_venta = costo × (1 + margen / 100). El margen es el porcentaje de PARAMETRO (MARGEN_PRODUCTO_PCT
	 * o MARGEN_PREMIUM_PCT) que el servicio lee con ParametroApi; la encargada puede ajustarlo antes de guardar.
	 */
	public void calcularPrecioVenta(BigDecimal margen) {
		if (margen == null || margen.signum() < 0) {
			throw new ReglaNegocioException("RN14", "El margen sobre el costo es mayor o igual a 0.");
		}
		BigDecimal factor = BigDecimal.ONE.add(margen.divide(CIEN, 6, RoundingMode.HALF_UP));
		this.precioVenta = costo.multiply(factor).setScale(2, RoundingMode.HALF_UP);
	}

	/** RF86, RN16: el mínimo y la ubicación son por sede; el stock nace en 0 y solo lo cambia el kardex. */
	public void definirStockMinimo(Long idSede, Integer stockMinimo, String ubicacion) {
		if (stockMinimo == null || stockMinimo < 0) {
			throw new ReglaNegocioException("RN16", "El stock mínimo es mayor o igual a 0.");
		}
		inventarioDe(idSede).ifPresentOrElse(inventario -> inventario.definirMinimo(stockMinimo, ubicacion),
				() -> inventarios.add(new InventarioSede(this, idSede, stockMinimo, ubicacion)));
	}

	/**
	 * RF46, RN15, RN18: salida de insumo, ligada a la atención si la hubo (CUS33, y CUS11 al finalizar la atención).
	 * Después de guardar, ultimoMovimiento(idSede) da su id (la regularización de CUS33 lo guarda en CONSUMO_PENDIENTE).
	 */
	public void registrarSalida(Long idSede, Integer cantidad, String motivo, Long idAtencion) {
		if (cantidad == null || cantidad <= 0) {
			throw new ReglaNegocioException("RN18", "La cantidad de la salida es mayor que 0.");
		}
		int stock = stockActual(idSede);
		if (cantidad > stock) {
			throw new ReglaNegocioException("RN15", "No hay stock suficiente: hay " + stock + " " + unidad + ".", HttpStatus.CONFLICT);
		}
		movimientos.add(MovimientoKardex.salida(this, idSede, cantidad, motivo, idAtencion));
	}

	/**
	 * RF48, RN17: cantidad = unidades contadas. El AJUSTE guarda la diferencia con signo (contado − stock_actual);
	 * si es 0 no se registra nada (CK_MOV_KARDEX_CANTIDAD) y devuelve false.
	 */
	public boolean registrarAjuste(Long idSede, Integer cantidad, String motivo, Long idUsuario) {
		if (cantidad == null || cantidad < 0) {
			throw new ReglaNegocioException("RF48", "Las unidades contadas son 0 o más.");
		}
		if (motivo == null || motivo.isBlank()) {
			throw new ReglaNegocioException("RF48", "Indique el motivo del ajuste.");
		}
		int diferencia = cantidad - stockActual(idSede);
		if (diferencia == 0) {
			return false;
		}
		movimientos.add(MovimientoKardex.ajuste(this, idSede, diferencia, motivo, idUsuario));
		return true;
	}

	/** RF47: el stock llegó al mínimo de la sede (se llama después de guardar el movimiento). */
	public boolean estaEnMinimo(Long idSede) {
		return inventarioDe(idSede).map(inventario -> stockActual(idSede) <= inventario.getStockMinimo()).orElse(false);
	}

	/** El último movimiento de la sede guardado en esta transacción, con su id y su saldo (CUS33, RN53). */
	public Optional<MovimientoKardex> ultimoMovimiento(Long idSede) {
		return Optional.ofNullable(guardadosPorSede.get(idSede));
	}

	/** Lo llama MovimientoKardex después de insertarse (y de que Hibernate vuelva a leer su saldo). */
	void alGuardarMovimiento(MovimientoKardex movimiento) {
		guardadosPorSede.merge(movimiento.getIdSede(), movimiento,
				(anterior, nuevo) -> nuevo.getIdMovimiento() > anterior.getIdMovimiento() ? nuevo : anterior);
	}

	/**
	 * El stock lo mueve TRG_KARDEX_APLICAR, no Java: después de un movimiento guardado vale su saldo, que Oracle calcula
	 * y Hibernate vuelve a leer; si no hubo ninguno, el stock de INVENTARIO_SEDE leído en la transacción.
	 */
	private int stockActual(Long idSede) {
		return ultimoMovimiento(idSede).map(MovimientoKardex::getSaldo)
			.orElseGet(() -> inventarioDe(idSede).map(InventarioSede::getStockActual).orElse(0));
	}

	private Optional<InventarioSede> inventarioDe(Long idSede) {
		return inventarios.stream().filter(inventario -> idSede.equals(inventario.getIdSede())).findFirst();
	}

	public List<InventarioSede> getInventarios() {
		return Collections.unmodifiableList(inventarios);
	}

	public List<MovimientoKardex> getMovimientos() {
		return Collections.unmodifiableList(movimientos);
	}

}
