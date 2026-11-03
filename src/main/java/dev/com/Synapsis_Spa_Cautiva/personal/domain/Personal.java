package dev.com.Synapsis_Spa_Cautiva.personal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Subtipo de PERSONA (seguridad): idPersonal = idPersona. Datos cargados por database/15_datos_iteracion1.sql.
 */
@Entity
@Table(name = "PERSONAL")
@Getter
public class Personal {

	@Id
	@Column(name = "id_personal")
	private Long idPersonal;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Enumerated(EnumType.STRING)
	@Column(name = "cargo", nullable = false, length = 30)
	private CargoPersonal cargo;

	@Enumerated(EnumType.STRING)
	@Column(name = "modalidad_pago", nullable = false, length = 10)
	private ModalidadPagoPersonal modalidadPago;

	@Column(name = "sueldo_base", nullable = false, precision = 10, scale = 2)
	private BigDecimal sueldoBase;

	@Column(name = "porcentaje_comision", nullable = false, precision = 5, scale = 2)
	private BigDecimal porcentajeComision;

	@Column(name = "fecha_ingreso", nullable = false)
	private LocalDate fechaIngreso;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoPersonal estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "personal", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PersonalArea> areas = new ArrayList<>();

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "personal", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HorarioPersonal> horarios = new ArrayList<>();

	protected Personal() {
	}

	public List<PersonalArea> getAreas() {
		return Collections.unmodifiableList(areas);
	}

	public List<HorarioPersonal> getHorarios() {
		return Collections.unmodifiableList(horarios);
	}

}
