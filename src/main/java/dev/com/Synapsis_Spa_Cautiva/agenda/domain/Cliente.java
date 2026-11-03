package dev.com.Synapsis_Spa_Cautiva.agenda.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.SiNoConverter;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Subtipo de PERSONA (seguridad): idCliente = idPersona y los datos comunes solo se leen por PersonaApi.
 */
@Entity
@Table(name = "CLIENTE")
@Getter
public class Cliente {

	@Id
	@Column(name = "id_cliente")
	private Long idCliente;

	@Column(name = "fecha_cumpleanos")
	private LocalDate fechaCumpleanos;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "acepta_comunicaciones", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean aceptaComunicaciones;

	@Column(name = "fecha_consentimiento", nullable = false)
	private LocalDateTime fechaConsentimiento;

	@Column(name = "observaciones", length = 300)
	private String observaciones;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoCliente estado;

	@Getter(AccessLevel.NONE)
	@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SaldoFavorMov> movimientosSaldo = new ArrayList<>();

	protected Cliente() {
	}

	public Cliente(Long idPersona, LocalDate fechaCumpleanos, String observaciones) {
		this.idCliente = idPersona;
		this.fechaCumpleanos = fechaCumpleanos;
		this.observaciones = observaciones;
		this.aceptaComunicaciones = Boolean.FALSE;
		this.fechaConsentimiento = LocalDateTime.now();
		this.estado = EstadoCliente.ACTIVO;
	}

	public void registrarConsentimiento(Boolean aceptaComunicaciones, LocalDateTime fechaConsentimiento) {
		this.aceptaComunicaciones = aceptaComunicaciones;
		this.fechaConsentimiento = fechaConsentimiento;
	}

	public void usarSaldo(BigDecimal monto, Long idVenta) {
		movimientosSaldo.add(new SaldoFavorMov(this, idVenta, monto));
	}

	public List<SaldoFavorMov> getMovimientosSaldo() {
		return Collections.unmodifiableList(movimientosSaldo);
	}

}
