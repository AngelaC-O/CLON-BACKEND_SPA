package dev.com.Synapsis_Spa_Cautiva.comun.api;

import java.math.BigDecimal;

/** SPI: los valores de PARAMETRO por su código exacto. Lo implementa seguridad. */
public interface ParametroApi {

	BigDecimal valorNumero(String codigo);

}
