package dev.com.Synapsis_Spa_Cautiva.agenda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.com.Synapsis_Spa_Cautiva.agenda.api.SaldoFavorApi;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.ClienteDTO;
import dev.com.Synapsis_Spa_Cautiva.agenda.controller.MapperCliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;
import dev.com.Synapsis_Spa_Cautiva.agenda.repository.RepositorioCliente;
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

	private final MapperCliente mapperCliente;

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

	/** Sin transacción, igual que PersonaApi: la llamada externa no debe retener una conexión de la base. */
	public ResultadoConsulta consultarDocumento(Long idTipoDocumento, String numeroDocumento) {
		return personaApi.consultarDocumento(idTipoDocumento, numeroDocumento);
	}

	/** Si la persona ya existe y aún no es clienta (por ejemplo, es del personal), solo se agrega CLIENTE: la jerarquía es solapada. */
	@Transactional
	public ClienteDTO registrar(ClienteDTO oClienteDTO) {
		Long idPersona = personaApi.registrarPersona(new PersonaDTO(null, NATURAL, oClienteDTO.nombres(), oClienteDTO.apellidos(), null,
				oClienteDTO.idTipoDocumento(), oClienteDTO.numeroDocumento(), oClienteDTO.telefono(), oClienteDTO.correo(), null,
				oClienteDTO.fechaVerificacion()));
		if (repositorioCliente.existsById(idPersona)) {
			throw new ReglaNegocioException("RF05", "El teléfono o el documento ya pertenecen a otra ficha.", HttpStatus.CONFLICT);
		}
		Cliente oCliente = new Cliente(idPersona, oClienteDTO.fechaCumpleanos(), oClienteDTO.observaciones());
		oCliente.registrarConsentimiento(oClienteDTO.aceptaComunicaciones(), LocalDateTime.now());
		return mapperCliente.aDTO(repositorioCliente.save(oCliente), personaApi.obtenerPersona(idPersona));
	}

	@Override
	@Transactional
	public void usarSaldo(Long idCliente, BigDecimal saldoFavorAplicado, Long idVenta) {
		Cliente oCliente = repositorioCliente.findById(idCliente)
			.orElseThrow(() -> new ReglaNegocioException("RF25", "La clienta " + idCliente + " no existe."));
		oCliente.usarSaldo(saldoFavorAplicado, idVenta);
		repositorioCliente.saveAndFlush(oCliente);
	}

}
