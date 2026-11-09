package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.math.BigDecimal;

/** SPI: los valores de PARAMETRO por su código exacto. Lo implementa seguridad. */
public interface ParametroApi {

	/** El nombre de la Iteración 1; lo siguen usando sus casos de uso (DF-80). */
	BigDecimal valorNumero(String codigo);

	/** [It. 2] El nombre de los diagramas (CUS05 con RN51, CUS32). Devuelve lo mismo que valorNumero. */
	BigDecimal obtenerValorNumero(String codigo);

}
