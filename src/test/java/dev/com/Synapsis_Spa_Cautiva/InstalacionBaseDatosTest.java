package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class InstalacionBaseDatosTest extends OracleTestcontainer {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void instalaLas57TablasDelModeloV52SinObjetosInvalidos() throws SQLException {
		try (Connection dueno = DriverManager.getConnection(ORACLE.getJdbcUrl(), DUENO, CLAVE_DUENO);
				Statement sentencia = dueno.createStatement()) {
			assertThat(contar(sentencia, "SELECT COUNT(*) FROM user_tables")).isEqualTo(57);
			assertThat(contar(sentencia,
					"SELECT COUNT(*) FROM user_constraints WHERE constraint_type = 'R' AND constraint_name NOT LIKE 'BIN$%'"))
				.isEqualTo(132);
			assertThat(contar(sentencia, "SELECT COUNT(*) FROM user_objects WHERE status = 'INVALID'")).isZero();
			// [It. 2.1 · v5.2] RECURSO sin tiempo de limpieza (RN03) y TRANSACCION_PASARELA con id_venta (FK_TRANS_PAS_VENTA, la FK 132)
			assertThat(contar(sentencia,
					"SELECT COUNT(*) FROM user_tab_columns WHERE table_name = 'RECURSO' AND column_name = 'MINUTOS_LIMPIEZA'")).isZero();
			assertThat(contar(sentencia, "SELECT COUNT(*) FROM user_constraints WHERE constraint_name = 'FK_TRANS_PAS_VENTA'")).isEqualTo(1);
		}
	}

	@Test
	void laAplicacionEntraComoCautivaAppConElEsquemaDelDueno() {
		String sesion = jdbcTemplate.queryForObject(
				"SELECT USER || '/' || SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM dual", String.class);

		assertThat(sesion).isEqualTo("CAUTIVA_APP/" + DUENO);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM USUARIO WHERE nombre_usuario = 'admin'", Integer.class))
			.isEqualTo(1);
	}

	private static int contar(Statement sentencia, String consulta) throws SQLException {
		try (ResultSet resultado = sentencia.executeQuery(consulta)) {
			resultado.next();
			return resultado.getInt(1);
		}
	}

}
