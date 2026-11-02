package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioClientePaquete;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.Caja;
import dev.com.Synapsis_Spa_Cautiva.caja.repository.RepositorioCaja;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Area;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.LineaNegocio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Servicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoRecurso;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioArea;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioServicio;
import dev.com.Synapsis_Spa_Cautiva.pagos.repository.RepositorioTransaccionPasarela;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Recurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.repository.RepositorioRecurso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Permiso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Persona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Rol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoDocumento;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoPersona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Usuario;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioPermiso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioPersona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioRol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioTipoDocumento;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioUsuario;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioVenta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arranca el contexto como CAUTIVA_APP con ddl-auto=validate. Cada prueba corre en una transacción que se revierte,
 * con PKG_SESION iniciado como la Administradora, porque Oracle lo exige.
 */
@SpringBootTest
@Transactional
class MapeoEntidadesTest extends OracleTestcontainer {

	private static final int TABLAS_DE_LA_ITERACION = 44;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RepositorioUsuario repositorioUsuario;

	@Autowired
	private RepositorioPersona repositorioPersona;

	@Autowired
	private RepositorioRol repositorioRol;

	@Autowired
	private RepositorioPermiso repositorioPermiso;

	@Autowired
	private RepositorioCita repositorioCita;

	@Autowired
	private RepositorioAtencion repositorioAtencion;

	@Autowired
	private RepositorioClientePaquete repositorioClientePaquete;

	@Autowired
	private RepositorioCaja repositorioCaja;

	@Autowired
	private RepositorioVenta repositorioVenta;

	@Autowired
	private RepositorioTransaccionPasarela repositorioTransaccionPasarela;

	@Autowired
	private RepositorioArea repositorioArea;

	@Autowired
	private RepositorioServicio repositorioServicio;

	@Autowired
	private RepositorioCliente repositorioCliente;

	@Autowired
	private RepositorioRecurso repositorioRecurso;

	@Autowired
	private RepositorioTipoDocumento repositorioTipoDocumento;

	private Long idAdmin;

	private Long idSede;

	@BeforeEach
	void iniciarSesionOracle() {
		idAdmin = repositorioUsuario.findByNombreUsuario("admin").orElseThrow().getIdUsuario();
		idSede = jdbcTemplate.queryForObject("SELECT MIN(id_sede) FROM SEDE", Long.class);
		jdbcTemplate.update("BEGIN PKG_SESION.iniciar(?); END;", idAdmin);
	}

	@Test
	void mapeaLasTablasDeLaIteracion() {
		assertThat(entityManager.getMetamodel().getEntities()).hasSize(TABLAS_DE_LA_ITERACION);
	}

	@Test
	void lasConsultasDeLosRepositoriosCorrenEnOracle() {
		LocalDate hoy = LocalDate.now();
		assertThat(repositorioCita.findOcupacion(idSede, hoy)).isEmpty();
		assertThat(repositorioAtencion.findOcupacionSinCita(idSede, hoy)).isEmpty();
		assertThat(repositorioClientePaquete.findActivoPorCliente(1L, 1L)).isEmpty();
		assertThat(repositorioCaja.existsAbiertaBySede(idSede)).isFalse();
		assertThat(repositorioTransaccionPasarela.findByCodigoOperacion("NO-EXISTE")).isEmpty();
		assertThat(repositorioPersona.findByDocumentoOTelefono(1L, "70000001", null)).hasSize(1);
		assertThat(repositorioPersona.findByNombresOTelefono("Administradora", null)).isNotEmpty();
		assertThat(repositorioUsuario.findRolesActivos()).hasSize(4);
		assertThat(repositorioUsuario.existsByNombreUsuario("admin")).isTrue();
		assertThat(repositorioUsuario.findPermisosPorRol(1L)).isNotNull();
	}

	@Test
	void autenticarUsaLaClaveBcryptDeLosDatosIniciales() {
		Usuario admin = repositorioUsuario.findByNombreUsuario("admin").orElseThrow();
		assertThat(admin.autenticar("CambiarAlEntrar2026")).isTrue();
		assertThat(admin.autenticar("otraClave")).isFalse();
	}

	@Test
	void oracleAsignaSerieNumeroYFechaDelComprobante() {
		Caja caja = repositorioCaja.saveAndFlush(new Caja(idSede, idAdmin, BigDecimal.TEN));
		Venta venta = new Venta(idSede, caja.getIdCaja(), null, idAdmin);
		venta.generarComprobante(TipoComprobante.BOLETA, null);
		repositorioVenta.saveAndFlush(venta);

		Comprobante comprobante = venta.getComprobantes().getFirst();
		assertThat(comprobante.getIdComprobante()).isNotNull();
		assertThat(comprobante.getSerie()).isEqualTo("B001");
		assertThat(comprobante.getNumero()).isNotBlank();
		assertThat(comprobante.getFechaEmision()).isNotNull();
	}

	@Test
	void oracleCalculaElEsperadoDelArqueo() {
		Long idEfectivo = jdbcTemplate.queryForObject("SELECT id_medio_pago FROM MEDIO_PAGO WHERE nombre = 'EFECTIVO'", Long.class);
		Caja caja = new Caja(idSede, idAdmin, new BigDecimal("100.00"));
		repositorioCaja.saveAndFlush(caja);
		caja.registrarEgreso("Compra menor", new BigDecimal("10.00"), idAdmin);
		// Hibernate inserta los hijos en orden alfabético del atributo (arqueos antes que egresos): el egreso va primero
		repositorioCaja.saveAndFlush(caja);
		caja.registrarArqueo(idEfectivo, new BigDecimal("90.00"), null);
		repositorioCaja.saveAndFlush(caja);

		assertThat(caja.getArqueos().getFirst().getMontoEsperado()).isEqualByComparingTo("90.00");
		assertThat(caja.getArqueos().getFirst().getDiferencia()).isEqualByComparingTo("0.00");
	}

	@Test
	void oracleCalculaElFinDeLaCitaConLaLimpiezaDelRecurso() {
		Persona persona = repositorioPersona.save(new Persona(TipoPersona.NATURAL, "Clienta", "De Mapeo", null, null, "987654321", null));
		repositorioCliente.saveAndFlush(new Cliente(persona.getIdPersona(), null, null));
		Long idEspecialista = jdbcTemplate.queryForObject(
				"SELECT id_personal FROM USUARIO WHERE nombre_usuario = 'especialista2'", Long.class);
		Long idCuidadoFacial = jdbcTemplate.queryForObject("SELECT id_area FROM AREA WHERE nombre = 'Cuidado Facial'", Long.class);
		Recurso recurso = repositorioRecurso.saveAndFlush(new Recurso(idSede, "Cabina de mapeo",
				dev.com.Synapsis_Spa_Cautiva.sedes.domain.TipoRecurso.CABINA, 15));
		Servicio servicio = new Servicio(idCuidadoFacial, "Limpieza de mapeo", 30, TipoRecurso.CABINA);
		servicio.agregarVariante("Única", new BigDecimal("50.00"));
		repositorioServicio.saveAndFlush(servicio);
		LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
		Cita cita = new Cita(persona.getIdPersona(), idSede, idEspecialista, recurso.getIdRecurso(),
				servicio.getVariantes().getFirst().getIdVariante(), lunes.atTime(10, 0), 30, idAdmin);
		repositorioCita.saveAndFlush(cita);

		assertThat(cita.getFechaHoraFin()).isEqualTo(lunes.atTime(10, 45));
		assertThat(repositorioCita.findOcupacion(idSede, lunes)).containsExactly(cita);

		cita.iniciarAtencion(40);
		repositorioCita.saveAndFlush(cita);
		assertThat(cita.getFechaHoraFin()).isEqualTo(lunes.atTime(10, 55));
	}

	@Test
	void validarNumeroAplicaLasReglasDeRn45() {
		Long idDni = jdbcTemplate.queryForObject("SELECT id_tipo_documento FROM TIPO_DOCUMENTO WHERE codigo_sunat = '1'", Long.class);
		TipoDocumento dni = repositorioTipoDocumento.findById(idDni).orElseThrow();
		assertThat(dni.validarNumero("70000001")).isTrue();
		assertThat(dni.validarNumero("7000")).isFalse();
		assertThat(dni.validarNumero("7000000A")).isFalse();
	}

	@Test
	void laComposicionGuardaHijosConPkCompuesta() {
		Rol rol = repositorioRol.findById(1L).orElseThrow();
		Permiso permiso = repositorioPermiso.findOrCreate("mapeo", "probar");
		rol.concederPermiso(permiso.getIdPermiso());
		repositorioRol.saveAndFlush(rol);
		assertThat(repositorioUsuario.findPermisosPorRol(1L)).contains(permiso);

		Area area = repositorioArea.save(new Area(LineaNegocio.SPA, "Área de prueba de mapeo"));
		Servicio servicio = new Servicio(area.getIdArea(), "Servicio de prueba", 30, TipoRecurso.CABINA);
		servicio.agregarVariante("Única", new BigDecimal("50.00"));
		repositorioServicio.saveAndFlush(servicio);
		assertThat(servicio.getVariantes().getFirst().getIdVariante()).isNotNull();

	}

}
