package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = MapperAtencion.class)
public interface MapperCliente {

	ClienteDTO aDTO(Cliente oCliente, PersonaDTO oPersonaDTO);

	/** [It. 2] CUS03: una atención del historial, con sus recomendaciones de producto (CUS12). */
	FichaClienteDTO.AtencionFicha aFicha(Atencion oAtencion);

}
