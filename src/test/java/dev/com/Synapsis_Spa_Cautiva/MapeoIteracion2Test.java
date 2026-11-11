package dev.com.Synapsis_Spa_Cautiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.EstadoListaEspera;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ListaEspera;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioListaEspera;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.Caja;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.Conciliacion;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.ConciliacionItem;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoCaja;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoConciliacion;
import dev.com.Synapsis_Spa_Cautiva.caja.domain.EstadoConciliacionItem;
import dev.com.Synapsis_Spa_Cautiva.caja.repository.RepositorioCaja;
import dev.com.Synapsis_Spa_Cautiva.caja.repository.RepositorioConciliacion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.EstadoServicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Paquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Promocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.Servicio;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoDescuentoPromocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.domain.TipoPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioPaquete;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioPromocion;
import dev.com.Synapsis_Spa_Cautiva.catalogo.repository.RepositorioServicio;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.CategoriaProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.ConsumoPendiente;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.EstadoConsumoPendiente;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.MovimientoKardex;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.Producto;
import dev.com.Synapsis_Spa_Cautiva.inventario.domain.TipoUsoProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.repository.RepositorioCategoriaProducto;
import dev.com.Synapsis_Spa_Cautiva.inventario.repository.RepositorioConsumoPendiente;
import dev.com.Synapsis_Spa_Cautiva.inventario.repository.RepositorioProducto;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.EstadoMedioPago;
import dev.com.Synapsis_Spa_Cautiva.pagos.domain.MedioPago;
import dev.com.Synapsis_Spa_Cautiva.pagos.repository.RepositorioMedioPago;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.Recurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.domain.TipoRecurso;
import dev.com.Synapsis_Spa_Cautiva.sedes.repository.RepositorioRecurso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.EstadoTipoDocumento;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Parametro;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Persona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Rol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoDocumento;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoPersona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Usuario;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioParametro;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioPermiso;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioPersona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioRol;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioTipoDocumento;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioUsuario;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.ComprobanteSunatDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.api.VentaResumenDTO;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Comprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.EstadoSolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.FormaDevolucion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.SolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.TipoComprobante;
import dev.com.Synapsis_Spa_Cautiva.ventas.domain.Venta;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioSolicitudAnulacion;
import dev.com.Synapsis_Spa_Cautiva.ventas.repository.RepositorioVenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agregados de la Iteración 2 contra Oracle: cada prueba guarda con los métodos de negocio y comprueba lo que Oracle
 * completa (@Generated). Corre como la Administradora en una transacción que se revierte, como MapeoEntidadesTest.
 */
@SpringBootTest
@Transactional
class MapeoIteracion2Test extends OracleTestcontainer {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private RepositorioUsuario repositorioUsuario;

	@Autowired
	private RepositorioPersona repositorioPersona;

	@Autowired
	private RepositorioRol repositorioRol;

	@Autowired
	private RepositorioPermiso repositorioPermiso;

	@Autowired
	private RepositorioParametro repositorioParametro;

	@Autowired
	private RepositorioTipoDocumento repositorioTipoDocumento;

	@Autowired
	private RepositorioCliente repositorioCliente;

	@Autowired
	private RepositorioCita repositorioCita;

	@Autowired
	private RepositorioAtencion repositorioAtencion;

	@Autowired
	private RepositorioListaEspera repositorioListaEspera;

	@Autowired
	private RepositorioRecurso repositorioRecurso;

	@Autowired
	private RepositorioServicio repositorioServicio;

	@Autowired
	private RepositorioPaquete repositorioPaquete;

	@Autowired
	private RepositorioPromocion repositorioPromocion;

	@Autowired
	private RepositorioCategoriaProducto repositorioCategoriaProducto;

	@Autowired
	private RepositorioProducto repositorioProducto;

	@Autowired
	private RepositorioConsumoPendiente repositorioConsumoPendiente;

	@Autowired
	private RepositorioCaja repositorioCaja;

	@Autowired
	private RepositorioConciliacion repositorioConciliacion;

	@Autowired
	private RepositorioVenta repositorioVenta;

	@Autowired
	private RepositorioSolicitudAnulacion repositorioSolicitudAnulacion;

	@Autowired
	private RepositorioMedioPago repositorioMedioPago;

	private Long idAdmin;

	private Long idSede;

	private final LocalDate hoy = LocalDate.now();

	@BeforeEach
	void iniciarSesionOracle() {
		idAdmin = repositorioUsuario.findByNombreUsuario("admin").orElseThrow().getIdUsuario();
		idSede = jdbc.queryForObject("SELECT MIN(id_sede) FROM SEDE", Long.class);
		jdbc.update("BEGIN PKG_SESION.iniciar(?); END;", idAdmin);
	}

	@Test
	void catalogoGuardaPaquetesYPromocionesConSusServicios() {
		Long idServicio = id("SELECT id_servicio FROM SERVICIO WHERE nombre = 'Corte de Cabello Caballero'");
		Paquete paquete = new Paquete("Paquete de mapeo", TipoPaquete.PAQUETE, 3, new BigDecimal("90.00"), 60);
		paquete.incluirServicio(idServicio);
		paquete.incluirServicio(idServicio);
		repositorioPaquete.saveAndFlush(paquete);
		assertThat(paquete.getServicios()).hasSize(1);
		assertThat(paquete.getServicios().getFirst().getIdPaquete()).isEqualTo(paquete.getIdPaquete());
		assertThat(repositorioPaquete.findByTipo(TipoPaquete.PAQUETE)).contains(paquete);

		Promocion promocion = new Promocion("Promoción de mapeo", TipoDescuentoPromocion.PORCENTAJE, BigDecimal.TEN, hoy, hoy.plusDays(7), idAdmin);
		promocion.aplicarAServicio(idServicio);
		repositorioPromocion.saveAndFlush(promocion);
		assertThat(promocion.getServicios().getFirst().getIdPromocion()).isEqualTo(promocion.getIdPromocion());
		assertThat(repositorioPromocion.findVigentes(hoy)).contains(promocion);
		assertThatThrownBy(() -> new Promocion("Inválida", TipoDescuentoPromocion.PORCENTAJE, new BigDecimal("150"), hoy, hoy, idAdmin))
			.isInstanceOf(ReglaNegocioException.class);

		Servicio servicio = repositorioServicio.findById(idServicio).orElseThrow();
		Long idVariante = servicio.getVariantes().getFirst().getIdVariante();
		servicio.actualizarDatos(servicio.getNombre(), 35, servicio.getTipoRecurso(), EstadoServicio.ACTIVO);
		servicio.cambiarPrecio(idVariante, new BigDecimal("45.00"));
		repositorioServicio.saveAndFlush(servicio);
		assertThat(id("SELECT precio FROM VARIANTE_SERVICIO WHERE id_variante = " + idVariante)).isEqualTo(45L);
	}

	@Test
	void inventarioRegistraKardexRecomendacionYConsumoPendiente() {
		Long idCategoria = repositorioCategoriaProducto.saveAndFlush(new CategoriaProducto("Categoría de mapeo")).getIdCategoria();
		Producto insumo = new Producto("MAP-INS", "Insumo de mapeo", null, idCategoria, "ML", TipoUsoProducto.INSUMO, new BigDecimal("10.00"));
		insumo.calcularPrecioVenta(new BigDecimal("50"));
		insumo.definirStockMinimo(idSede, 2, "Estante A");
		repositorioProducto.saveAndFlush(insumo);
		assertThat(insumo.getPrecioVenta()).isEqualByComparingTo("15.00");
		assertThat(insumo.getInventarios().getFirst().getStockActual()).isZero();

		assertThat(insumo.registrarAjuste(idSede, 5, "Conteo inicial", idAdmin)).isTrue();
		repositorioProducto.saveAndFlush(insumo);
		MovimientoKardex ajuste = insumo.ultimoMovimiento(idSede).orElseThrow();
		assertThat(ajuste.getSaldo()).isEqualTo(5);
		assertThat(ajuste.getCantidad()).isEqualTo(5);
		assertThat(insumo.registrarAjuste(idSede, 5, "Sin diferencia", idAdmin)).isFalse();

		insumo.registrarSalida(idSede, 4, "Uso interno", null);
		repositorioProducto.saveAndFlush(insumo);
		MovimientoKardex salida = insumo.ultimoMovimiento(idSede).orElseThrow();
		assertThat(salida.getIdMovimiento()).isGreaterThan(ajuste.getIdMovimiento());
		assertThat(salida.getSaldo()).isEqualTo(1);
		assertThat(salida.getIdUsuario()).isEqualTo(idAdmin);
		assertThat(insumo.estaEnMinimo(idSede)).isTrue();
		assertThatThrownBy(() -> insumo.registrarSalida(idSede, 9, "Más de lo que hay", null)).isInstanceOf(ReglaNegocioException.class);
		assertThat(repositorioProducto.findMovimientos(insumo.getIdProducto(), idSede, hoy, hoy)).hasSize(2);

		Long idEspecialista = id("SELECT id_personal FROM USUARIO WHERE nombre_usuario = 'especialista2'");
		Long idVariante = id("SELECT v.id_variante FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio"
				+ " WHERE s.nombre = 'Limpieza Facial Profunda Estándar'");
		Recurso cabina = repositorioRecurso.saveAndFlush(new Recurso(idSede, "Cabina de mapeo It2", TipoRecurso.CABINA, 10));
		Atencion atencion = repositorioAtencion.saveAndFlush(
				new Atencion(null, "Clienta de paso", idEspecialista, cabina.getIdRecurso(), idVariante, 30));
		atencion.recomendarProducto(insumo.getIdProducto(), "Aplicar de noche");
		repositorioAtencion.saveAndFlush(atencion);
		assertThat(atencion.getRecomendaciones().getFirst().getIdRecomendacion()).isNotNull();
		assertThatThrownBy(() -> atencion.recomendarProducto(insumo.getIdProducto(), null)).isInstanceOf(ReglaNegocioException.class);

		ConsumoPendiente consumo = repositorioConsumoPendiente.saveAndFlush(
				new ConsumoPendiente(atencion.getIdAtencion(), insumo.getIdProducto(), idSede, 3, 1));
		assertThat(consumo.getCantidadPendiente()).isEqualTo(2);
		assertThat(repositorioConsumoPendiente.findPendientes(idSede)).contains(consumo);
		consumo.descartar(idAdmin, "Se usó menos de lo estimado");
		repositorioConsumoPendiente.saveAndFlush(consumo);
		assertThat(consumo.getEstado()).isEqualTo(EstadoConsumoPendiente.DESCARTADO);
		assertThat(consumo.getIdUsuarioResuelve()).isEqualTo(idAdmin);
		assertThat(consumo.getFechaResolucion()).isNotNull();
	}

	@Test
	void ventaDeProductoSolicitudDeAnulacionCajaYConciliacion() {
		Long idCategoria = id("SELECT id_categoria FROM CATEGORIA_PRODUCTO WHERE nombre = 'Cuidado Capilar'");
		Producto producto = new Producto("MAP-VEN", "Producto de mapeo", "Marca", idCategoria, "UND", TipoUsoProducto.VENTA,
				new BigDecimal("20.00"));
		producto.calcularPrecioVenta(new BigDecimal("50"));
		producto.definirStockMinimo(idSede, 1, null);
		repositorioProducto.saveAndFlush(producto);
		producto.registrarAjuste(idSede, 5, "Stock inicial", idAdmin);
		repositorioProducto.saveAndFlush(producto);

		Caja caja = repositorioCaja.saveAndFlush(new Caja(idSede, idAdmin, new BigDecimal("100.00")));
		assertThat(caja.getIdUsuarioApertura()).isEqualTo(idAdmin);
		Venta venta = new Venta(idSede, caja.getIdCaja(), null, idAdmin);
		venta.agregarProducto(producto.getIdProducto(), 2, producto.getPrecioVenta(), null);
		repositorioVenta.saveAndFlush(venta);
		venta.registrarPago(id("SELECT id_medio_pago FROM MEDIO_PAGO WHERE nombre = 'EFECTIVO'"), new BigDecimal("60.00"), null);
		venta.generarComprobante(TipoComprobante.BOLETA, null);
		venta.cerrar();
		repositorioVenta.saveAndFlush(venta);
		assertThat(venta.getDetalles().getFirst().getPrecioUnitario()).isEqualByComparingTo("30.00");
		assertThat(venta.getIdUsuarioAutoriza()).isNull();
		assertThat(id("SELECT stock_actual FROM INVENTARIO_SEDE WHERE id_producto = " + producto.getIdProducto() + " AND id_sede = " + idSede))
			.isEqualTo(3L);

		SolicitudAnulacion solicitud = repositorioSolicitudAnulacion.saveAndFlush(
				new SolicitudAnulacion(venta.getIdVenta(), "Cobro de prueba", "01", FormaDevolucion.MEDIO_ORIGINAL, idAdmin));
		assertThat(solicitud.getIdUsuarioSolicita()).isEqualTo(idAdmin);
		assertThat(solicitud.getFechaSolicitud()).isNotNull();
		assertThat(repositorioSolicitudAnulacion.findPendientes(idSede)).contains(solicitud);
		solicitud.rechazar(idAdmin, "La venta es correcta");
		repositorioSolicitudAnulacion.saveAndFlush(solicitud);
		assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAnulacion.RECHAZADA);
		assertThat(solicitud.getIdUsuarioResuelve()).isEqualTo(idAdmin);
		assertThatThrownBy(() -> venta.anular(idAdmin)).isInstanceOf(ReglaNegocioException.class);

		for (MedioPago medio : repositorioMedioPago.findAll()) {
			if (medio.getEstado() == EstadoMedioPago.ACTIVO) {
				caja.registrarArqueo(medio.getIdMedioPago(), BigDecimal.ZERO, "Arqueo de la prueba de mapeo");
			}
		}
		repositorioCaja.saveAndFlush(caja);
		caja.cerrar(idAdmin);
		repositorioCaja.saveAndFlush(caja);
		assertThat(caja.getFechaCierre()).isNotNull();
		assertThat(repositorioCaja.findCerradasConDiferencia(idSede)).contains(caja);
		assertThat(repositorioCaja.findDelDia(idSede, LocalDateTime.now())).contains(caja);
		caja.aprobarRevision(idAdmin, LocalDateTime.now());
		repositorioCaja.saveAndFlush(caja);
		assertThat(caja.getIdUsuarioRevisa()).isEqualTo(idAdmin);
		caja.reabrir(idAdmin, "Venta tardía de la prueba de mapeo");
		repositorioCaja.saveAndFlush(caja);
		assertThat(caja.getEstado()).isEqualTo(EstadoCaja.ABIERTA);
		assertThat(caja.getIdUsuarioReapertura()).isEqualTo(idAdmin);
		assertThat(caja.getFechaReapertura()).isNotNull();
		assertThat(caja.getFechaCierre()).isNull();
		assertThat(caja.getIdUsuarioRevisa()).isNull();

		Comprobante boleta = venta.getComprobantes().getFirst();
		Conciliacion conciliacion = new Conciliacion(2099, 12, idAdmin);
		conciliacion.generarItems(
				List.of(new VentaResumenDTO(venta.getIdVenta(), idSede, hoy, "BOLETA", boleta.getSerie(), boleta.getNumero(), new BigDecimal("60.00"))),
				List.of(new ComprobanteSunatDTO(venta.getIdVenta(), "BOLETA", boleta.getSerie(), boleta.getNumero(), new BigDecimal("60.00"),
						"CERRADA", "ACEPTADO", null, null, null),
						new ComprobanteSunatDTO(null, "BOLETA", "B001", "99999999", BigDecimal.ONE, "CERRADA", "ACEPTADO", null, null, null)),
				List.of());
		repositorioConciliacion.saveAndFlush(conciliacion);
		assertThat(conciliacion.getItems()).extracting(ConciliacionItem::getEstado)
			.containsExactly(EstadoConciliacionItem.COINCIDE, EstadoConciliacionItem.SIN_JUSTIFICAR);
		assertThatThrownBy(() -> conciliacion.cerrar(LocalDateTime.now())).isInstanceOf(ReglaNegocioException.class);
		conciliacion.justificarItem(conciliacion.getItems().get(1).getIdItem(), EstadoConciliacionItem.JUSTIFICADA, "Boleta de contingencia");
		conciliacion.cerrar(LocalDateTime.now());
		repositorioConciliacion.saveAndFlush(conciliacion);
		assertThat(conciliacion.getEstado()).isEqualTo(EstadoConciliacion.CERRADA);
	}

	@Test
	void agendaReprogramaCancelaAbonaYAsignaLaEspera() {
		Persona persona = repositorioPersona.save(new Persona(TipoPersona.NATURAL, "Clienta", "Iteración Dos", null, null, "987600111", null));
		Cliente cliente = repositorioCliente.saveAndFlush(new Cliente(persona.getIdPersona(), null, null));
		cliente.actualizarDatos(LocalDate.of(1990, 5, 20), "Prefiere la mañana");
		repositorioCliente.saveAndFlush(cliente);

		Long idEspecialista = id("SELECT id_personal FROM USUARIO WHERE nombre_usuario = 'especialista1'");
		Long idVariante = id("SELECT v.id_variante FROM VARIANTE_SERVICIO v JOIN SERVICIO s ON s.id_servicio = v.id_servicio"
				+ " WHERE s.nombre = 'Corte de Cabello Caballero'");
		Recurso sillon = repositorioRecurso.saveAndFlush(new Recurso(idSede, "Sillón de mapeo It2", TipoRecurso.SILLON, 10));
		Caja caja = repositorioCaja.saveAndFlush(new Caja(idSede, idAdmin, BigDecimal.TEN));
		LocalDate lunes = hoy.with(TemporalAdjusters.next(DayOfWeek.MONDAY));

		Cita cita = new Cita(cliente.getIdCliente(), idSede, idEspecialista, sillon.getIdRecurso(), idVariante, lunes.atTime(9, 0), 30, idAdmin);
		cita.registrarAdelanto(new BigDecimal("20.00"), id("SELECT id_medio_pago FROM MEDIO_PAGO WHERE nombre = 'EFECTIVO'"),
				LocalDateTime.now(), caja.getIdCaja());
		repositorioCita.saveAndFlush(cita);
		assertThat(cita.getIdUsuario()).isEqualTo(idAdmin);

		Cita nueva = cita.reprogramar(lunes.atTime(11, 0), idEspecialista, sillon.getIdRecurso());
		repositorioCita.saveAndFlush(cita);
		repositorioCita.saveAndFlush(nueva);
		assertThat(cita.getEstado()).isEqualTo(EstadoCita.REPROGRAMADA);
		assertThat(nueva.getIdCitaOrigen()).isEqualTo(cita.getIdCita());
		assertThat(nueva.getAdelantoMonto()).isEqualByComparingTo("20.00");
		assertThat(repositorioCita.findPendientesPorCliente(cliente.getIdCliente())).containsExactly(nueva);

		nueva.cancelar("No se presentó");
		repositorioCita.saveAndFlush(nueva);
		cliente.abonarSaldo(nueva.getAdelantoMonto(), nueva.getIdCita(), "Cita cancelada: no se presentó");
		repositorioCliente.saveAndFlush(cliente);
		assertThat(repositorioCliente.saldoFavorDisponible(cliente.getIdCliente())).isEqualByComparingTo("20.00");
		assertThatThrownBy(() -> nueva.cancelar("Otra vez")).isInstanceOf(ReglaNegocioException.class);

		ListaEspera espera = repositorioListaEspera.saveAndFlush(
				new ListaEspera(cliente.getIdCliente(), idSede, idVariante, lunes, "09:00", "12:00", idAdmin));
		assertThat(repositorioListaEspera.findPendientes(idSede, lunes)).contains(espera);
		espera.asignarCita(nueva.getIdCita());
		repositorioListaEspera.saveAndFlush(espera);
		assertThat(espera.getEstado()).isEqualTo(EstadoListaEspera.ASIGNADA);

		assertThatThrownBy(() -> nueva.verificarEspecialista(-1L, false)).isInstanceOf(ReglaNegocioException.class);
		nueva.verificarEspecialista(-1L, true);
		nueva.verificarEspecialista(idEspecialista, false);
	}

	@Test
	void seguridadYConfiguracionRespetanSusReglas() {
		Rol administradora = repositorioRol.findAll().stream().filter(rol -> rol.getNombre().equals("ADMINISTRADORA")).findFirst().orElseThrow();
		List<Long> seguridad = List.of(repositorioPermiso.findOrCreate("SEGURIDAD", "VER").getIdPermiso(),
				repositorioPermiso.findOrCreate("SEGURIDAD", "CREAR").getIdPermiso(),
				repositorioPermiso.findOrCreate("SEGURIDAD", "EDITAR").getIdPermiso());
		administradora.verificarPermisosMinimos(seguridad);
		administradora.revocarPermiso(seguridad.get(2));
		assertThatThrownBy(() -> administradora.verificarPermisosMinimos(seguridad)).isInstanceOf(ReglaNegocioException.class);

		Usuario especialista = repositorioUsuario.findByNombreUsuario("especialista2").orElseThrow();
		especialista.restablecerClave("$2a$10$0000000000000000000000000000000000000000000000000000.");
		repositorioUsuario.saveAndFlush(especialista);
		assertThat(especialista.getDebeCambiarClave()).isTrue();
		assertThat(especialista.getIntentosFallidos()).isZero();

		Parametro paso = repositorioParametro.findByCodigo("AGENDA_PASO_MIN").orElseThrow();
		paso.cambiarValor(new BigDecimal("20"), null, null, null);
		repositorioParametro.saveAndFlush(paso);
		assertThat(paso.getIdUsuario()).isEqualTo(idAdmin);
		assertThat(paso.getFechaVigencia()).isEqualTo(hoy);

		TipoDocumento pasaporte = repositorioTipoDocumento.findByCodigoSunat("7").orElseThrow();
		pasaporte.configurar(Boolean.FALSE, EstadoTipoDocumento.INACTIVO);
		repositorioTipoDocumento.saveAndFlush(pasaporte);
		assertThat(pasaporte.validarNumero("AB123456")).isFalse();

		MedioPago efectivo = repositorioMedioPago.findAll().stream().filter(medio -> medio.getNombre().equals("EFECTIVO")).findFirst().orElseThrow();
		assertThatThrownBy(() -> efectivo.configurar(EstadoMedioPago.ACTIVO, Boolean.TRUE)).isInstanceOf(ReglaNegocioException.class);
		assertThatThrownBy(() -> new Recurso(idSede, "Sillón de 30 minutos", TipoRecurso.SILLON, 30)).isInstanceOf(ReglaNegocioException.class);
	}

	private Long id(String consulta) {
		return jdbc.queryForObject(consulta, Long.class);
	}

}
