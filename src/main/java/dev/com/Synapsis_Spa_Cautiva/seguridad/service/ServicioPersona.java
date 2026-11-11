package dev.com.Synapsis_Spa_Cautiva.seguridad.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.com.Synapsis_Spa_Cautiva.comun.api.ReglaNegocioException;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaApi;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.ResultadoConsulta;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.Persona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoDocumento;
import dev.com.Synapsis_Spa_Cautiva.seguridad.domain.TipoPersona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioPersona;
import dev.com.Synapsis_Spa_Cautiva.seguridad.repository.RepositorioTipoDocumento;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ServicioPersona implements PersonaApi {

	private static final String ENCONTRADO = "ENCONTRADO";

	private final RepositorioTipoDocumento repositorioTipoDocumento;

	private final ConsultaDocumentoPort consultaDocumentoPort;

	private final RepositorioPersona repositorioPersona;

	@Override
	@Transactional(readOnly = true)
	public List<PersonaDTO> buscarPersonas(String nombres, String telefono) {
		return repositorioPersona.findByNombresOTelefono(nombres, telefono).stream().map(this::aDTO).toList();
	}

	/** Sin transacción: la llamada externa no debe retener una conexión de la base. */
	@Override
	public ResultadoConsulta consultarDocumento(Long idTipoDocumento, String numeroDocumento) {
		TipoDocumento oTipoDocumento = validarDocumento(idTipoDocumento, numeroDocumento);
		if (!oTipoDocumento.getConsultaApi()) {
			throw new ReglaNegocioException("RN45", "El " + oTipoDocumento.getAbreviatura() + " no se consulta: ingrese los datos a mano.");
		}
		DatosPersona oDatosPersona = consultaDocumentoPort.consultar(oTipoDocumento.getCodigoSunat(), numeroDocumento);
		LocalDateTime fechaVerificacion = ENCONTRADO.equals(oDatosPersona.resultado()) ? LocalDateTime.now() : null;
		return new ResultadoConsulta(idTipoDocumento, oDatosPersona.resultado(), oDatosPersona.nombres(), oDatosPersona.apellidos(),
				oDatosPersona.razonSocial(), fechaVerificacion);
	}

	@Override
	@Transactional(readOnly = true)
	public String obtenerCodigoSunat(Long idTipoDocumento) {
		return repositorioTipoDocumento.findById(idTipoDocumento)
			.map(TipoDocumento::getCodigoSunat)
			.orElseThrow(() -> new ReglaNegocioException("RN45", "El tipo de documento " + idTipoDocumento + " no existe."));
	}

	@Override
	@Transactional(readOnly = true)
	public Long obtenerIdTipoDocumento(String codigoSunat) {
		return repositorioTipoDocumento.findByCodigoSunat(codigoSunat)
			.map(TipoDocumento::getIdTipoDocumento)
			.orElseThrow(() -> new ReglaNegocioException("RN45", "No existe el tipo de documento con código " + codigoSunat + "."));
	}

	@Override
	@Transactional
	public Long registrarPersona(PersonaDTO oPersonaDTO) {
		validarDocumentoOpcional(oPersonaDTO);
		List<Persona> existentes = repositorioPersona.findByDocumentoOTelefono(oPersonaDTO.idTipoDocumento(), oPersonaDTO.numeroDocumento(),
				oPersonaDTO.telefono());
		if (existentes.size() > 1) {
			throw new ReglaNegocioException("RF05", "El documento y el teléfono pertenecen a fichas distintas.", HttpStatus.CONFLICT);
		}
		if (!existentes.isEmpty()) {
			return existentes.getFirst().getIdPersona();
		}
		TipoPersona tipoPersona = oPersonaDTO.tipoPersona() == null ? TipoPersona.NATURAL : TipoPersona.valueOf(oPersonaDTO.tipoPersona());
		Persona oPersona = new Persona(tipoPersona, oPersonaDTO.nombres(), oPersonaDTO.apellidos(), oPersonaDTO.idTipoDocumento(),
				oPersonaDTO.numeroDocumento(), oPersonaDTO.telefono(), oPersonaDTO.correo());
		if (oPersonaDTO.fechaVerificacion() != null) {
			oPersona.marcarVerificado(oPersonaDTO.fechaVerificacion());
		}
		return repositorioPersona.save(oPersona).getIdPersona();
	}

	@Override
	@Transactional
	public PersonaDTO actualizarPersona(Long idPersona, PersonaDTO oPersonaDTO) {
		validarDocumentoOpcional(oPersonaDTO);
		Persona oPersona = buscar(idPersona);
		boolean deOtra = repositorioPersona.findByDocumentoOTelefono(oPersonaDTO.idTipoDocumento(), oPersonaDTO.numeroDocumento(), oPersonaDTO.telefono())
			.stream()
			.anyMatch(existente -> !existente.getIdPersona().equals(idPersona));
		if (deOtra) {
			throw new ReglaNegocioException("RF05", "El teléfono o el documento ya pertenecen a otra ficha.", HttpStatus.CONFLICT);
		}
		oPersona.actualizar(oPersonaDTO.nombres(), oPersonaDTO.apellidos(), oPersonaDTO.idTipoDocumento(), oPersonaDTO.numeroDocumento(),
				oPersonaDTO.telefono(), oPersonaDTO.correo());
		if (oPersonaDTO.fechaVerificacion() != null && !oPersonaDTO.fechaVerificacion().equals(oPersona.getFechaVerificacion())) {
			oPersona.marcarVerificado(oPersonaDTO.fechaVerificacion());
		}
		return aDTO(repositorioPersona.saveAndFlush(oPersona));
	}

	@Override
	@Transactional(readOnly = true)
	public PersonaDTO obtenerPersona(Long idPersona) {
		return aDTO(buscar(idPersona));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PersonaDTO> obtenerPersonas(List<Long> idsPersona) {
		if (idsPersona.isEmpty()) {
			return List.of();
		}
		return repositorioPersona.findAllById(idsPersona.stream().distinct().toList()).stream().map(this::aDTO).toList();
	}

	private Persona buscar(Long idPersona) {
		return repositorioPersona.findById(idPersona)
			.orElseThrow(() -> new ReglaNegocioException("RN01", "La persona " + idPersona + " no existe.", HttpStatus.NOT_FOUND));
	}

	private void validarDocumentoOpcional(PersonaDTO oPersonaDTO) {
		if (oPersonaDTO.idTipoDocumento() != null || oPersonaDTO.numeroDocumento() != null) {
			validarDocumento(oPersonaDTO.idTipoDocumento(), oPersonaDTO.numeroDocumento());
		}
	}

	private TipoDocumento validarDocumento(Long idTipoDocumento, String numeroDocumento) {
		if (idTipoDocumento == null) {
			throw new ReglaNegocioException("RN45", "Indique el tipo de documento.");
		}
		TipoDocumento oTipoDocumento = repositorioTipoDocumento.findById(idTipoDocumento)
			.orElseThrow(() -> new ReglaNegocioException("RN45", "El tipo de documento " + idTipoDocumento + " no existe.", HttpStatus.NOT_FOUND));
		if (!oTipoDocumento.validarNumero(numeroDocumento)) {
			throw new ReglaNegocioException("RN45", "El número no cumple el formato de " + oTipoDocumento.getAbreviatura() + ".");
		}
		return oTipoDocumento;
	}

	private PersonaDTO aDTO(Persona oPersona) {
		return new PersonaDTO(oPersona.getIdPersona(), oPersona.getTipoPersona().name(), oPersona.getNombres(), oPersona.getApellidos(),
				oPersona.getRazonSocial(), oPersona.getIdTipoDocumento(), oPersona.getNumeroDocumento(), oPersona.getTelefono(), oPersona.getCorreo(),
				oPersona.getOrigenDatos().name(), oPersona.getFechaVerificacion());
	}

}
