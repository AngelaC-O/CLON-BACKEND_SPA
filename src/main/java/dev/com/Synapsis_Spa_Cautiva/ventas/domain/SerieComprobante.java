package dev.com.Synapsis_Spa_Cautiva.ventas.domain;

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
@Table(name = "SERIE_COMPROBANTE")
@Getter
public class SerieComprobante {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_serie")
	private Long idSerie;

	@Column(name = "id_sede", nullable = false)
	private Long idSede;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 12)
	private TipoComprobante tipo;

	@Column(name = "serie", nullable = false, length = 4)
	private String serie;

	@Column(name = "ultimo_numero", nullable = false)
	private Integer ultimoNumero;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoSerieComprobante estado;

	protected SerieComprobante() {
	}

}
