package dev.com.Synapsis_Spa_Cautiva.agenda.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "LISTA_ESPERA")
@Getter
public class ListaEspera {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_espera")
	private Long idEspera;

	@Column(name = "id_cliente", nullable = false)
	private Long idCliente;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Column(name = "id_variante", nullable = false)
	private Long idVariante;

	@Column(name = "fecha_deseada", nullable = false)
	private LocalDate fechaDeseada;

	@Column(name = "hora_desde", length = 5)
	private String horaDesde;

	@Column(name = "hora_hasta", length = 5)
	private String horaHasta;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoListaEspera estado;

	@Column(name = "id_cita")
	private Long idCita;

	@Column(name = "id_usuario", nullable = false)
	private Long idUsuario;

	@Column(name = "fecha_registro", nullable = false)
	private LocalDateTime fechaRegistro;

	protected ListaEspera() {
	}

	public ListaEspera(Long idCliente, Long idSede, Long idVariante, LocalDate fechaDeseada, String horaDesde, String horaHasta,
			Long idUsuario) {
		this.idCliente = idCliente;
		this.idSede = idSede;
		this.idVariante = idVariante;
		this.fechaDeseada = fechaDeseada;
		this.horaDesde = horaDesde;
		this.horaHasta = horaHasta;
		this.idUsuario = idUsuario;
		this.estado = EstadoListaEspera.PENDIENTE;
		this.fechaRegistro = LocalDateTime.now();
	}

}
