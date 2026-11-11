package dev.com.Synapsis_Spa_Cautiva.seguridad.api;

import java.util.List;

/** Los datos comunes de PERSONA: CLIENTE, PERSONAL y PROVEEDOR solo los leen y escriben por aquí. */
public interface PersonaApi {

	List<PersonaDTO> buscarPersonas(String nombres, String telefono);

	ResultadoConsulta consultarDocumento(Long idTipoDocumento, String numeroDocumento);

	Long obtenerIdTipoDocumento(String codigoSunat);

	String obtenerCodigoSunat(Long idTipoDocumento);

	/** Si la persona ya existe con ese documento o teléfono, devuelve su id: un subtipo nuevo comparte la misma PERSONA. */
	Long registrarPersona(PersonaDTO oPersonaDTO);

	/**
	 * [It. 2 · CUS04] Devuelve la persona como quedó: si el teléfono o el documento son de otra persona responde 409 (RF05); si el documento
	 * cambia sin una consulta nueva (fechaVerificacion), Oracle la deja MANUAL (TRG_PERSONA_REGLAS, RN45).
	 */
	PersonaDTO actualizarPersona(Long idPersona, PersonaDTO oPersonaDTO);

	PersonaDTO obtenerPersona(Long idPersona);

	/** [It. 2] Varias a la vez, para poner nombres en listas (CUS08, CUS52). Las que no existen no vienen. */
	List<PersonaDTO> obtenerPersonas(List<Long> idsPersona);

}
