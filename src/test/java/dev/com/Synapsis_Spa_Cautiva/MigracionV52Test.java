package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Migración v5.1 → v5.2 (database/18_ajustes_v52.sql, Iteración 2.1): corre database/22_pruebas_v52.sql en el mismo
 * contenedor, después de 18. El script prueba la agenda sin tiempo de limpieza (RN03), la reapertura de la caja del día y su
 * auditoría, la nota de crédito de una solicitud, los cobros de una venta (TRANSACCION_PASARELA.id_venta, -20107) y las
 * columnas nuevas de V_VENTA_LINEA y V_KARDEX, y termina en ROLLBACK.
 */
class MigracionV52Test extends OracleTestcontainer {

	@Test
	void lasReglasDeLaMigracionV52PasanSinFallos() {
		String salida = sqlplusComoDueno("@/database/22_pruebas_v52.sql");

		assertThat(salida).as(salida).contains("Resultado: ").contains(" OK, 0 FALLO").doesNotContain("FALLO  ");
		// la salida de sqlplus llega en Windows-1252: se evitan las tildes
		assertThat(salida).contains("OK     RECURSO ya no tiene minutos_limpieza (RN03)")
			.contains("OK     V_AGENDA_BLOQUES tiene la columna FIN y ya no FIN_CON_LIMPIEZA")
			.contains("OK     Misma especialista a la hora de fin (10:30) en otro sill")
			.contains("OK     Mismo sill").contains("n a la hora de fin (10:30) con otra especialista")
			.contains("OK     Desde hora_fin, el mismo sill")
			.contains("OK     La Administradora reabre con motivo una caja de otro d")
			.contains("OK     AUDITORIA de CAJA guarda el motivo")
			.contains("es la recepcionista de la solicitud, no la Administradora (D-21)")
			.contains("OK     Cobro con id_venta y con id_cita (CK_TRANS_PAS_VENTA)")
			.contains("OK     Cambiar el id_venta del cobro")
			.contains("OK     Pago de la venta Y con la transacci")
			.contains("OK     V_VENTA_LINEA trae id_producto (producto) e id_variante (servicio) (D-37)")
			.contains("OK     V_KARDEX trae el comprobante en la salida de la venta");
	}

}
