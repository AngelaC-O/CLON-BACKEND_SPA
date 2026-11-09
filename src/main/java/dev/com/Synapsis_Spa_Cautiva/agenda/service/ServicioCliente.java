package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.SaldoFavorApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.ClienteDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.FichaClienteDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.MapperCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.MapperCliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cita;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.ClientePaquete;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioAtencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCita;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioClientePaquete;
import dev.com.Synapsis_Spa_Cautiva.comun.api.HistorialComprasProvider;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ProductoCompradoDTO;
import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.ResultadoConsulta;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioCliente implements SaldoFavorApi {

	private static final String NATURAL = "NATURAL";

	private final PersonaApi personaApi;

	private final RepositorioCliente repositorioCliente;

	private final RepositorioAtencion repositorioAtencion;

	private final RepositorioCita repositorioCita;

	private final RepositorioClientePaquete repositorioClientePaquete;

	private final HistorialComprasProvider historialComprasProvider;

	private final MapperCliente mapperCliente;

	private final MapperCita mapperCita;

	@Transactional(readOnly = true)
	public List<ClienteDTO> buscar(String nombres, String telefono) {
		List<PersonaDTO> personas = personaApi.buscarPersonas(nombres, telefono);
		Map<Long, Cliente> clientes = repositorioCliente.findAllById(personas.stream().map(PersonaDTO::idPersona).toList()).stream()
			.collect(Collectors.toMap(Cliente::getIdCliente, Function.identity()));
		return personas.stream()
			.filter(oPersonaDTO -> clientes.containsKey(oPersonaDTO.idPersona()))
			.map(oPersonaDTO -> mapperCliente.aDTO(clientes.get(oPersonaDTO.idPersona()), oPersonaDTO))
			.toList();
	}

	/**
	 * [It. 2 · CUS03, RF02] La ficha, en el orden del diagrama: PERSONA (PersonaApi), CLIENTE, saldo a favor (V_SALDO_FAVOR_CLIENTE, RN05),
	 * atenciones, citas pendientes, paquetes con sesiones (V_SESIONES_PAQUETE, RN36) y productos comprados (HistorialComprasProvider, de ventas).
	 */
	@Transactional(readOnly = true)
	public FichaClienteDTO obtenerFicha(Long idCliente) {
		PersonaDTO oPersonaDTO = personaApi.obtenerPersona(idCliente);
		Cliente oCliente = buscarCliente(idCliente);
		BigDecimal saldoDisponible = repositorioCliente.saldoFavorDisponible(idCliente);
		List<Atencion> atenciones = repositorioAtencion.findByCliente(idCliente);
		List<Cita> citas = repositorioCita.findPendientesPorCliente(idCliente);
		List<ClientePaquete> paquetes = repositorioClientePaquete.findActivosPorCliente(idCliente);
		List<ProductoCompradoDTO> productos = historialComprasProvider.productosComprados(idCliente);
		return new FichaClienteDTO(mapperCliente.aDTO(oCliente, oPersonaDTO), saldoDisponible, atenciones.stream().map(mapperCliente::aFicha).toList(),
				citas.stream().map(mapperCita::aDTO).toList(), paquetes.stream().map(this::aFicha).toList(), productos);
	}

	/** Sin transacción, igual que PersonaApi: la llamada externa no debe retener una conexión de la base. */
	public ResultadoConsulta consultarDocumento(Long idTipoDocumento, String numeroDocumento) {
		return personaApi.consultarDocumento(idTipoDocumento, numeroDocumento);
	}

	/** Si la persona ya existe y aún no es clienta (por ejemplo, es del personal), solo se agrega CLIENTE: la jerarquía es solapada. */
	@Transactional
	public ClienteDTO registrar(ClienteDTO oClienteDTO) {
		Long idPersona = personaApi.registrarPersona(aPersona(null, oClienteDTO));
		if (repositorioCliente.existsById(idPersona)) {
			throw new ReglaNegocioException("RF05", "El teléfono o el documento ya pertenecen a otra ficha.", HttpStatus.CONFLICT);
		}
		Cliente oCliente = new Cliente(idPersona, oClienteDTO.fechaCumpleanos(), oClienteDTO.observaciones());
		oCliente.registrarConsentimiento(oClienteDTO.aceptaComunicaciones(), LocalDateTime.now());
		return mapperCliente.aDTO(repositorioCliente.save(oCliente), personaApi.obtenerPersona(idPersona));
	}

	/**
	 * [It. 2 · CUS04, RF02] Los datos comunes van por PersonaApi.actualizarPersona (teléfono o documento de otra persona = 409, RF05; documento
	 * cambiado sin consulta nueva = MANUAL, RN45). Agenda solo cambia cumpleaños, observaciones y consentimiento; la fecha del consentimiento
	 * cambia solo si cambia la respuesta (RN27).
	 */
	@Transactional
	public ClienteDTO actualizar(Long idCliente, ClienteDTO oClienteDTO) {
		PersonaDTO oPersonaDTO = personaApi.actualizarPersona(idCliente, aPersona(idCliente, oClienteDTO));
		Cliente oCliente = buscarCliente(idCliente);
		oCliente.actualizarDatos(oClienteDTO.fechaCumpleanos(), oClienteDTO.observaciones());
		LocalDateTime fechaConsentimiento = oClienteDTO.aceptaComunicaciones().equals(oCliente.getAceptaComunicaciones())
				? oCliente.getFechaConsentimiento()
				: LocalDateTime.now();
		oCliente.registrarConsentimiento(oClienteDTO.aceptaComunicaciones(), fechaConsentimiento);
		return mapperCliente.aDTO(repositorioCliente.save(oCliente), oPersonaDTO);
	}

	@Override
	@Transactional
	public void usarSaldo(Long idCliente, BigDecimal saldoFavorAplicado, Long idVenta) {
		Cliente oCliente = repositorioCliente.findById(idCliente)
			.orElseThrow(() -> new ReglaNegocioException("RF25", "La clienta " + idCliente + " no existe."));
		oCliente.usarSaldo(saldoFavorAplicado, idVenta);
		repositorioCliente.saveAndFlush(oCliente);
	}

	private static PersonaDTO aPersona(Long idPersona, ClienteDTO oClienteDTO) {
		return new PersonaDTO(idPersona, NATURAL, oClienteDTO.nombres(), oClienteDTO.apellidos(), null, oClienteDTO.idTipoDocumento(),
				oClienteDTO.numeroDocumento(), oClienteDTO.telefono(), oClienteDTO.correo(), null, oClienteDTO.fechaVerificacion());
	}

	/** RN36: las sesiones usadas se cuentan en V_SESIONES_PAQUETE. */
	private FichaClienteDTO.PaqueteFicha aFicha(ClientePaquete oClientePaquete) {
		Integer usadas = repositorioClientePaquete.contarSesionesUsadas(oClientePaquete.getIdClientePaquete());
		int sesionesUsadas = usadas == null ? 0 : usadas;
		return new FichaClienteDTO.PaqueteFicha(oClientePaquete.getIdClientePaquete(), oClientePaquete.getIdPaquete(),
				oClientePaquete.getSesionesTotales(), sesionesUsadas, oClientePaquete.getSesionesTotales() - sesionesUsadas,
				oClientePaquete.getFechaCompra(), oClientePaquete.getFechaVencimiento(), oClientePaquete.getEstado());
	}

	private Cliente buscarCliente(Long idCliente) {
		return repositorioCliente.findById(idCliente)
			.orElseThrow(() -> new ReglaNegocioException("RF02", "La clienta " + idCliente + " no existe.", HttpStatus.NOT_FOUND));
	}

}
