package dev.com.Synapsis_Spa_Cautiva.seguridad.domain;

import java.util.regex.Pattern;

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
@Table(name = "TIPO_DOCUMENTO")
@Getter
public class TipoDocumento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_tipo_documento")
	private Long idTipoDocumento;

	@Column(name = "codigo_sunat", nullable = false, length = 1)
	private String codigoSunat;

	@Column(name = "nombre", nullable = false, length = 40)
	private String nombre;

	@Column(name = "abreviatura", nullable = false, length = 10)
	private String abreviatura;

	@Column(name = "longitud_min", nullable = false)
	private Integer longitudMin;

	@Column(name = "longitud_max", nullable = false)
	private Integer longitudMax;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "solo_numeros", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean soloNumeros;

	@Convert(converter = SiNoConverter.class)
	@Column(name = "consulta_api", nullable = false, columnDefinition = "CHAR(1)")
	private Boolean consultaApi;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoTipoDocumento estado;

	private static final String CODIGO_RUC = "6";
	private static final Pattern FORMATO_RUC = Pattern.compile("(10|15|17|20)[0-9]{9}");
	private static final Pattern SOLO_DIGITOS = Pattern.compile("[0-9]+");
	private static final Pattern ALFANUMERICO = Pattern.compile("[A-Z0-9]+");

	protected TipoDocumento() {
	}

	/** RN45: las mismas reglas de FN_VALIDAR_DOCUMENTO, para responder antes de llegar a Oracle. */
	public boolean validarNumero(String numeroDocumento) {
		if (estado != EstadoTipoDocumento.ACTIVO || numeroDocumento == null) {
			return false;
		}
		String numero = numeroDocumento.trim().toUpperCase();
		if (numero.length() < longitudMin || numero.length() > longitudMax) {
			return false;
		}
		if (CODIGO_RUC.equals(codigoSunat) && !FORMATO_RUC.matcher(numero).matches()) {
			return false;
		}
		return (soloNumeros ? SOLO_DIGITOS : ALFANUMERICO).matcher(numero).matches();
	}

}
