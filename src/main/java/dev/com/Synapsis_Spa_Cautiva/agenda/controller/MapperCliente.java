package dev.com.Synapsis_Spa_Cautiva.agenda.controller;

import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Atencion;
import dev.com.Synapsis_Spa_Cautiva.agenda.domain.Cliente;
import dev.com.Synapsis_Spa_Cautiva.seguridad.api.PersonaDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MapperAtencion.class)
public interface MapperCliente {

	ClienteDTO aDTO(Cliente oCliente, PersonaDTO oPersonaDTO);

	/** [It. 2] CUS03: una atención del historial, con sus recomendaciones de producto (CUS12). */
	@Mapping(target = "nombreServicio", ignore = true)
	@Mapping(target = "nombreEspecialista", ignore = true)
	@Mapping(target = "nombreRecurso", ignore = true)
	FichaClienteDTO.AtencionFicha aFicha(Atencion oAtencion);

}
