package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "PARAMETRO")
@Getter
public class Parametro {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_parametro")
	private Long idParametro;

	@Column(name = "codigo", nullable = false, length = 40)
	private String codigo;

	@Column(name = "descripcion", nullable = false, length = 150)
	private String descripcion;

	@Column(name = "valor_numero", precision = 10, scale = 2)
	private BigDecimal valorNumero;

	@Column(name = "valor_texto", length = 100)
	private String valorTexto;

	@Column(name = "fecha_vigencia", nullable = false)
	private LocalDate fechaVigencia;

	@Column(name = "id_usuario")
	private Long idUsuario;

	protected Parametro() {
	}

}
