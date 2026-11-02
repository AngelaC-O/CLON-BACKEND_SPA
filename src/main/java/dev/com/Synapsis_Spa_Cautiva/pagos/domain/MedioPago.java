package dev.com.Synapsis_Spa_Cautiva.pagos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.SiNoConverter;

import lombok.Getter;

@Entity
@Table(name = "MEDIO_PAGO")
@Getter
public class MedioPago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_medio_pago")
	private Long idMedioPago;

	@Column(name = "nombre", nullable = false, length = 15)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoMedioPago estado;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "usa_pasarela", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean usaPasarela;

	protected MedioPago() {
	}

}
