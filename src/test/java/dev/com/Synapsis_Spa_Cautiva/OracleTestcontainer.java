package dev.com.Synapsis_Spa_Cautiva;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Clase base de las pruebas de integración: un solo Oracle Free por ejecución, con database/
 * instalado como dice database/README.md. La aplicación se conecta como CAUTIVA_APP con
 * CURRENT_SCHEMA = dueño, igual que en los ambientes reales.
 */
@ActiveProfiles("test")
public abstract class OracleTestcontainer {

	protected static final String DUENO = "CAUTIVA_TEST";
	protected static final String CLAVE_DUENO = "ClaveContenedor2026";
	private static final String CUENTA_APP = "CAUTIVA_APP";
	private static final String CLAVE_APP = "ClaveAppContenedor2026";
	private static final String SERVICIO = "//localhost:1521/FREEPDB1";
	private static final Pattern ERROR_SQLPLUS = Pattern.compile("(ORA|PLS|SP2)-\\d+");

	protected static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-free:23.26.2")
			.withUsername(DUENO)
			.withPassword(CLAVE_DUENO)
			.withEnv("TZ", "America/Lima")
			.withStartupTimeout(Duration.ofMinutes(5))
			.withCopyFileToContainer(MountableFile.forHostPath("database"), "/database");

	static {
		ORACLE.start();
		instalarBaseDeDatos();
	}

	@DynamicPropertySource
	static void conexionDeLaAplicacion(DynamicPropertyRegistry registry) {
		registry.add("CAUTIVA_DB_URL", ORACLE::getJdbcUrl);
		registry.add("CAUTIVA_DB_USER", () -> CUENTA_APP);
		registry.add("CAUTIVA_DB_PASSWORD", () -> CLAVE_APP);
		registry.add("CAUTIVA_DB_SCHEMA", () -> DUENO);
	}

	private static void instalarBaseDeDatos() {
		String dueno = DUENO + "/" + CLAVE_DUENO;
		String system = "system/" + CLAVE_DUENO;
		sqlplus(dueno, "@/database/00_instalar_v5.sql");
		sqlplus(system, "@/database/14_cuenta_aplicacion.sql");
		sqlplus(dueno, "@/database/13_permisos.sql");
		sqlplus(dueno, "@/database/15_datos_iteracion1.sql");
		sqlplus(system, "ALTER USER " + CUENTA_APP + " IDENTIFIED BY \"" + CLAVE_APP + "\" ACCOUNT UNLOCK;");
	}

	/** Corre en /tmp porque 00 hace SPOOL en la carpeta actual y /database no es escribible. */
	private static void sqlplus(String cuenta, String instruccion) {
		String comando = "cd /tmp && export NLS_LANG=AMERICAN_AMERICA.WE8MSWIN1252 && printf '%s\\nEXIT\\n' '" + instruccion
				+ "' | sqlplus -s -L " + cuenta + "@" + SERVICIO;
		try {
			ExecResult resultado = ORACLE.execInContainer("bash", "-c", comando);
			String salida = resultado.getStdout() + resultado.getStderr();
			if (resultado.getExitCode() != 0 || ERROR_SQLPLUS.matcher(salida).find()) {
				throw new IllegalStateException("Falló " + instruccion + ":\n" + salida);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("No se pudo ejecutar " + instruccion, ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Se interrumpió " + instruccion, ex);
		}
	}

}
