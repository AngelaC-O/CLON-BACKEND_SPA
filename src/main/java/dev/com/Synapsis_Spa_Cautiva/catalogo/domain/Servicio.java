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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "SERVICIO")
@Getter
public class Servicio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_servicio")
	private Long idServicio;

	@Column(name = "id_area", nullable = false)
	private Long idArea;

	@Column(name = "nombre", nullable = false, length = 80)
	private String nombre;

	@Column(name = "duracion_ref_min", nullable = false)
	private Integer duracionRefMin;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_recurso", nullable = false, length = 15)
	private TipoRecurso tipoRecurso;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoServicio estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VarianteServicio> variantes = new ArrayList<>();

	protected Servicio() {
	}

	public Servicio(Long idArea, String nombre, Integer duracionRefMin, TipoRecurso tipoRecurso) {
		this.idArea = idArea;
		this.nombre = nombre;
		this.duracionRefMin = duracionRefMin;
		this.tipoRecurso = tipoRecurso;
		this.estado = EstadoServicio.ACTIVO;
	}

	public void agregarVariante(String nombreVariante, BigDecimal precio) {
		if (precio == null || precio.signum() < 0) {
			throw new ReglaNegocioException("RN40", "Cada variante tiene un precio fijo mayor o igual a 0.");
		}
		variantes.add(new VarianteServicio(this, nombreVariante, precio));
	}

	/** RF83: desactivar un servicio es estado = INACTIVO; no se borra porque lo usan citas y ventas. */
	public void actualizarDatos(String nombre, Integer duracionRefMin, TipoRecurso tipoRecurso, EstadoServicio estado) {
		if (duracionRefMin == null || duracionRefMin < 5 || duracionRefMin > 600) {
			throw new ReglaNegocioException("RF83", "La duración de referencia va de 5 a 600 minutos.");
		}
		this.nombre = nombre;
		this.duracionRefMin = duracionRefMin;
		this.tipoRecurso = tipoRecurso;
		this.estado = estado;
	}

	/** Un insumo ya definido cambia su cantidad (CUS58); uno nuevo se agrega (CUS57). Va en su unidad mínima (RN54). */
	public void definirInsumo(Long idVariante, Long idProducto, BigDecimal cantidadEstimada) {
		buscarVariante(idVariante).definirInsumo(idProducto, cantidadEstimada);
	}

	/** RN25: el precio anterior y el nuevo los guarda TRG_AUD_VARIANTE_SERVICIO en AUDITORIA. */
	public void cambiarPrecio(Long idVariante, BigDecimal precioNuevo) {
		buscarVariante(idVariante).cambiarPrecio(precioNuevo);
	}

	private VarianteServicio buscarVariante(Long idVariante) {
		return variantes.stream()
			.filter(candidata -> idVariante.equals(candidata.getIdVariante()))
			.findFirst()
			.orElseThrow(() -> new ReglaNegocioException("RF49", "La variante " + idVariante + " no es de este servicio."));
	}

	public List<VarianteServicio> getVariantes() {
		return Collections.unmodifiableList(variantes);
	}

}
