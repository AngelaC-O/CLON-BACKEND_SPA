package dev.com.Synapsis_Spa_Cautiva.personal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;

@Entity
@Table(name = "HORARIO_PERSONAL")
@Getter
public class HorarioPersonal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_horario")
	private Long idHorario;

	@Getter(AccessLevel.NONE)
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_personal", nullable = false)
	private Personal personal;

	@Column(name = "dia_semana", nullable = false)
	private Integer diaSemana;

	@Column(name = "hora_entrada", nullable = false, length = 5)
	private String horaEntrada;

	@Column(name = "hora_salida", nullable = false, length = 5)
	private String horaSalida;

	@Column(name = "minutos_almuerzo", nullable = false)
	private Integer minutosAlmuerzo;

	protected HorarioPersonal() {
	}

	public Long getIdPersonal() {
		return personal.getIdPersonal();
	}

}
