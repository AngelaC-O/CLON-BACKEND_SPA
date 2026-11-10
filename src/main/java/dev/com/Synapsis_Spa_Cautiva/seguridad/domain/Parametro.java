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

	private static final String AGENDA_PASO_MIN = "AGENDA_PASO_MIN";

	private static final String LIMPIEZA_MAX_MIN = "LIMPIEZA_MAX_MIN";

	private static final BigDecimal LIMPIEZA_TOPE = BigDecimal.TEN;

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
		validarRango(valorNumero);
		this.valorNumero = valorNumero;
		this.valorTexto = sinTexto ? null : valorTexto.trim();
		this.fechaVigencia = fechaVigencia == null ? LocalDate.now() : fechaVigencia;
		this.idUsuario = idUsuario;
	}

	/**
	 * [It. 2 · CUS65] Los dos parámetros de la agenda con un rango del negocio: AGENDA_PASO_MIN es un entero mayor que 0 (RN51) y LIMPIEZA_MAX_MIN
	 * un entero de 0 a 10, el tope de CK_RECURSO_MINUTOS_LIMPIEZA (RN03). No está en el diagrama (DF-129).
	 */
	private void validarRango(BigDecimal valorNumero) {
		if (AGENDA_PASO_MIN.equals(codigo) && (!esEntero(valorNumero) || valorNumero.signum() <= 0)) {
			throw new ReglaNegocioException("RN51", "AGENDA_PASO_MIN es un número entero de minutos mayor que 0.");
		}
		if (LIMPIEZA_MAX_MIN.equals(codigo) && (!esEntero(valorNumero) || valorNumero.signum() < 0 || valorNumero.compareTo(LIMPIEZA_TOPE) > 0)) {
			throw new ReglaNegocioException("RN03", "LIMPIEZA_MAX_MIN es un número entero de minutos entre 0 y 10.");
		}
	}

	private static boolean esEntero(BigDecimal valor) {
		return valor != null && valor.stripTrailingZeros().scale() <= 0;
	}

}
