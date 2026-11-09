package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;

import lombok.Getter;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

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

	/** TRG_PARAMETRO_USUARIO pone el usuario de PKG_SESION. */
	@Generated(event = { EventType.INSERT, EventType.UPDATE }, writable = true)
	@Column(name = "id_usuario")
	private Long idUsuario;

	protected Parametro() {
	}

	/**
	 * RF87, RN14, RN22, RN29: la Administradora cambia el valor (TRG_ADM_PARAMETRO); el anterior y el nuevo quedan en
	 * AUDITORIA. Lleva valor numérico o de texto (CK_PARAMETRO_VALOR); sin fecha, rige desde hoy.
	 */
	public void cambiarValor(BigDecimal valorNumero, String valorTexto, LocalDate fechaVigencia, Long idUsuario) {
		boolean sinTexto = valorTexto == null || valorTexto.isBlank();
		if (valorNumero == null && sinTexto) {
			throw new ReglaNegocioException("RF87", "El parámetro " + codigo + " lleva un valor numérico o de texto.");
		}
		this.valorNumero = valorNumero;
		this.valorTexto = sinTexto ? null : valorTexto.trim();
		this.fechaVigencia = fechaVigencia == null ? LocalDate.now() : fechaVigencia;
		this.idUsuario = idUsuario;
	}

}
