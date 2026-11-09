package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Migración v5 → v5.1 (database/17_datos_iteracion2.sql): corre database/21_pruebas_v51.sql en el
 * mismo contenedor, después de 17. El script prueba las reglas nuevas de Oracle (reabrir la caja,
 * solicitud de anulación, consumo automático y consumo pendiente) y termina en ROLLBACK.
 */
class MigracionV51Test extends OracleTestcontainer {

	@Test
	void lasReglasDeLaMigracionV51PasanSinFallos() {
		String salida = sqlplusComoDueno("@/database/21_pruebas_v51.sql");

		assertThat(salida).as(salida).contains("Resultado: ").contains(" OK, 0 FALLO").doesNotContain("FALLO  ");
		assertThat(salida).contains("OK     Recepcionista reabre la caja con motivo")
			.contains("OK     La Administradora reabre la caja con motivo (CUS46)")
			.contains("OK     Segunda solicitud PENDIENTE de la misma venta (UQ_SOL_ANUL_PENDIENTE)")
			.contains("OK     Recepcionista aprueba la solicitud")
			.contains("OK     Salida autom") // la salida de sqlplus llega en Windows-1252: se evita la tilde
			.contains("de la especialista con id_atencion (consumo estimado, RN53)")
			.contains("OK     La misma salida sin id_atencion (RN17)")
			.contains("OK     Regularizar sin SALIDA de kardex");
	}

}
